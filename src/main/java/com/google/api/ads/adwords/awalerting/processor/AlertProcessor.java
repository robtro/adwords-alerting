// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.ads.adwords.awalerting.processor;

import com.google.api.ads.adwords.awalerting.AlertConfigLoadException;
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.authentication.Authenticator;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.awalerting.util.ManagedCustomerDelegate;
import com.google.api.ads.adwords.jaxws.v201705.mcm.ApiException;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Main reporting processor responsible for downloading report files to the file system, parsing
 * them into ReportData objects, applying alert rules and running alert actions.
 */
@Component
@Qualifier("alertProcessor")
public class AlertProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertProcessor.class);
  private static final String SEPARATOR = System.getProperty("line.separator");

  private static final int DEFAULT_NUM_THREADS = 20;
  private final int numThreads;

  private Authenticator authenticator;

  /**
   * Constructor with threads parameter autowired by Spring.
   *
   * @param numberOfThreads number of threads to be used
   */
  @Autowired
  public AlertProcessor(
      @Value(value = "${aw.alerting.processor.threads:}") Integer numberOfThreads) {
    if (numberOfThreads != null && numberOfThreads > 0) {
      this.numThreads = numberOfThreads;
    } else {
      this.numThreads = DEFAULT_NUM_THREADS;
      LOGGER.warn(
          "Config property \"aw.alerting.processor.threads\" not found, using default value {}",
          DEFAULT_NUM_THREADS);
    }
  }

  /**
   * Uses the API to retrieve the managed accounts, and extract their IDs.
   *
   * @param session the adwords session
   * @return the client customer IDs for all the managed accounts
   */
  private Set<Long> retrieveClientCustomerIds(AdWordsSession session)
      throws AlertProcessingException {
    try {
      LOGGER.info("Client customer IDs being recovered from the API. This may take a while...");
      return new ManagedCustomerDelegate(session).getClientCustomerIds();
    } catch (ApiException e) {
      throw new AlertProcessingException(
          "Encountered API error while getting client customer IDs.", e);
    }
  }

  /**
   * Generate all the alerts for the given account IDs under the manager account.
   *
   * @param clientCustomerIds the client customer IDs
   * @param alertsConfig the JSON config of the alerts
   */
  public void generateAlerts(Set<Long> clientCustomerIds, JsonObject alertsConfig)
      throws AlertConfigLoadException, AlertProcessingException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    
    ImmutableAdWordsSession session = null;
    try {
      session = authenticator.authenticate();
    } catch (OAuthException e) {
      throw new AlertConfigLoadException("Failed to authenticate AdWordsSession.", e);
    } catch (ValidationException e) {
      throw new AlertConfigLoadException("Failed to build AdWordsSession.", e);
    }
    
    if (clientCustomerIds == null) {
      clientCustomerIds = retrieveClientCustomerIds(session);

      // Write the client customer IDs into debug log.
      LOGGER.debug("Client customer IDs retrieved:{}{}",
          SEPARATOR, Joiner.on(SEPARATOR).join(clientCustomerIds));
    }

    int count = 0;
    for (JsonElement alertConfig : alertsConfig.getAsJsonArray(ConfigTags.ALERTS)) {
      count++;
      processAlert(clientCustomerIds, session, alertConfig.getAsJsonObject(), count);
    }

    stopwatch.stop();
    LOGGER.info(
        "*** Finished all processing in {} seconds ***",
        stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
  }

  /**
   * Process one alert for the given account IDs under the manager account.
   *
   * @param clientCustomerIds the client customer IDs
   * @param protoSession the prototype adwords session used for downloading reports
   * @param alertConfig the JSON config of the alert
   * @param count the sequence number of current alert
   */
  protected void processAlert(
      Set<Long> clientCustomerIds,
      ImmutableAdWordsSession protoSession,
      JsonObject alertConfig,
      int count)
      throws AlertConfigLoadException, AlertProcessingException {
    String alertName = alertConfig.get(ConfigTags.ALERT_NAME).getAsString();
    LOGGER.info("*** Generating alert #{} (name: \"{}\") for {} accounts ***",
        count, alertName, clientCustomerIds.size());

    JsonObject downloaderConfig = alertConfig.getAsJsonObject(ConfigTags.REPORT_DOWNLOADER);
    JsonArray rulesConfig = alertConfig.getAsJsonArray(ConfigTags.RULES); // optional
    String alertMessage = alertConfig.get(ConfigTags.ALERT_MESSAGE).getAsString();
    JsonArray actionsConfig = alertConfig.getAsJsonArray(ConfigTags.ACTIONS);

    // Generate AWQL report query and download report data for all accounts under manager account.
    List<ReportData> reports = downloadReports(protoSession, clientCustomerIds, downloaderConfig);
    printReports(reports, "*** Downloaded report data:");

    // Process the downloaded reports
    processReports(reports, rulesConfig, alertMessage, actionsConfig);
  }

  /**
   * Download report files for the given account IDs under the manager account.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param clientCustomerIds the client customer IDs
   * @param downloaderConfig the JSON config for this downloader
   */
  protected List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession,
      Set<Long> clientCustomerIds,
      JsonObject downloaderConfig)
      throws AlertConfigLoadException, AlertProcessingException {
    AlertReportDownloaderProcessor reportDownloadProcessor =
        new AlertReportDownloaderProcessor(downloaderConfig);
    return reportDownloadProcessor.downloadReports(protoSession, clientCustomerIds);
  }

  /**
   * Process reports for the given account IDs under the manager account.
   *
   * @param reports the downloaded reports
   * @param rulesConfig the JSON config of current alert rules
   * @param alertMessage the current alert message template
   * @param actionsConfig the JSON config of current alert actions
   */
  protected void processReports(
      List<ReportData> reports, JsonArray rulesConfig, String alertMessage, JsonArray actionsConfig)
      throws AlertProcessingException {
    if (reports == null || reports.isEmpty()) {
      LOGGER.info("No reports to process!");
      return;
    }

    LOGGER.info("*** Start processing reports...");
    Stopwatch stopwatch = Stopwatch.createStarted();

    applyAlertRulesAndMessages(reports, rulesConfig, alertMessage);
    printReports(reports, "*** Reports after processing alert rules and messages:");
    applyAlertActions(reports, actionsConfig);

    stopwatch.stop();
    LOGGER.info(
        "*** Finished processing all reports in {} seconds.",
        stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
  }

  /**
   * Apply alert rules on reports.
   *
   * @param reports the list of report data
   * @param rulesConfig the JSON config of the alert rules
   */
  private void applyAlertRulesAndMessages(
      List<ReportData> reports, JsonArray rulesConfig, String alertMessage)
      throws AlertProcessingException {
    AlertRulesProcessor rulesProcessor =
        new AlertRulesProcessor(rulesConfig, alertMessage, numThreads);
    rulesProcessor.processReports(reports);
  }

  /**
   * Apply alert actions on reports.
   *
   * @param reports the list of report data
   * @param actionsConfig the JSON config of the alert actions
   */
  private void applyAlertActions(List<ReportData> reports, JsonArray actionsConfig)
      throws AlertProcessingException {
    AlertActionsProcessor actionsProcessor = new AlertActionsProcessor(actionsConfig, numThreads);
    actionsProcessor.processReports(Collections.unmodifiableList(reports));
  }

  /**
   * Prints out the reports content (for debugging).
   *
   * @param reports the list of reports to print
   * @param caption the caption message
   */
  private void printReports(List<ReportData> reports, String caption) {
    if (LOGGER.isDebugEnabled()) {
      StringBuffer sb = new StringBuffer(caption);
      int seq = 1;
      for (ReportData report : reports) {
        sb.append(String.format("%n===== Report #[%s] =====%n", seq++));
        sb.append(report.toString());
      }
      LOGGER.debug(sb.toString());
    }
  }

  /**
   * Set Authenticator autowired by Spring
   *
   * @param authenticator the helper authenticator to build AdWordsSession
   */
  @Autowired
  public void setAuthentication(Authenticator authenticator) {
    this.authenticator = authenticator;
  }
}
