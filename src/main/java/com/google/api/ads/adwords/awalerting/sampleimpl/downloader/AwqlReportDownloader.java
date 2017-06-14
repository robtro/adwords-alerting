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

package com.google.api.ads.adwords.awalerting.sampleimpl.downloader;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that concurrently downloads report via the reporting API.
 * 
 * <p>An {@link ExecutorService} is created in order to handle all the threads. To initialize the
 * executor is necessary to call {@code initializeExecutorService}, and to finalize the executor
 * is necessary to call {@code finalizeExecutorService} after all the downloads are done, and the
 * downloader will not be used again.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "AwqlReportDownloader",
 *   "ReportQuery": {
 *     "ReportType": "...",
 *     "Fields": "...",
 *     "Conditions": "...",
 *     "DateRange": "..."
 *   }
 * }
 * </pre>
 */
public class AwqlReportDownloader implements AlertReportDownloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwqlReportDownloader.class);
  private static final String SEPARATOR = System.getProperty("line.separator");

  private static final String REPORT_QUERY_TAG = "ReportQuery";

  private static final int NUM_THREADS = 20;
  private int numThreads = NUM_THREADS;

  private final AwqlReportQuery reportQuery;
  
  public AwqlReportDownloader(JsonObject config) {
    JsonObject reportQueryConfig = config.getAsJsonObject(REPORT_QUERY_TAG);
    this.reportQuery = new AwqlReportQuery(reportQueryConfig);
  }

  /**
   * Downloads the specified report for all specified CIDs.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param clientCustomerIds the client customer IDs to download the report for
   * @return Collection of File objects reports have been downloaded to
   */
  @Override
  public List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession, Set<Long> clientCustomerIds)
      throws AlertProcessingException {
    ImmutableAdWordsSession session = null;
    try {
      // No need to specify clientCustomerId for getting report definition.
      session = buildSessionForCid(protoSession, null);
    } catch (ValidationException e) {
      throw new AlertProcessingException(
          "Failed to create valid adwords session for report defintion downloader.", e);
    }

    AwReportDefinitionDownloader reportDefinitionDownloader =
        new AwReportDefinitionDownloader(session);
    return downloadReports(protoSession, reportDefinitionDownloader, clientCustomerIds);
  }

  /**
   * Downloads the specified report for all specified CIDs, returns List<File> for all successful
   * downloads, and prints out list of failed CIDs.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param reportDefinitionDownloader report definition downloader (for getting fields mapping)
   * @param clientCustomerIds the client customer IDs to download the report for
   * @return Collection of File objects reports have been downloaded to
   */
  protected List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession,
      AwReportDefinitionDownloader reportDefinitionDownloader,
      Set<Long> clientCustomerIds)
      throws AlertProcessingException {
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    Stopwatch stopwatch = Stopwatch.createStarted();
    LOGGER.info("Downloading {} reports...", reportQuery.getReportType());
    
    ReportDefinitionReportType reportType = reportQuery.getReportTypeEnum();
    Map<String, String> fieldsMapping = reportDefinitionDownloader.getFieldsMapping(reportType);
    ReportDataLoader loader = new ReportDataLoader(reportType, fieldsMapping);
    
    List<Long> taskIds = new ArrayList<>();
    List<CallableAwqlReportDownloader> taskJobs = new ArrayList<>();
    Map<Long, String> failures = new HashMap<>();
    ImmutableAdWordsSession session = null;
    for (Long clientCustomerId : clientCustomerIds) {
      try {
        session = buildSessionForCid(protoSession, clientCustomerId);
      } catch (ValidationException e) {
        LOGGER.error(
            "Failed to create valid adwords session for CID {}, skipping it.", clientCustomerId);
        failures.put(clientCustomerId, e.getMessage());
        continue;
      }
      
      taskIds.add(clientCustomerId);
      taskJobs.add(genCallableAwqlReportDownloader(session, loader));
    }
    
    List<Future<ReportData>> taskResults;
    try {
      //Note that invokeAll() returns results in the same sequence as input tasks.
      taskResults = executorService.invokeAll(taskJobs);
    } catch (InterruptedException e) {
      throw new AlertProcessingException(
        "AwqlReportDownloader encounters InterruptedException.", e);
    }
    
    List<ReportData> results = new ArrayList<>();
    for (int i = 0; i < taskResults.size(); i++) {
      try {
        results.add(taskResults.get(i).get());
      } catch (ExecutionException e) {
        failures.put(taskIds.get(i), e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AlertProcessingException(
            "AwqlReportDownloader encounters InterruptedException.", e);
      }
    }

    executorService.shutdown();
    stopwatch.stop();

    LOGGER.info("Downloaded reports for {} accounts in {} seconds.",
        clientCustomerIds.size(), stopwatch.elapsed(TimeUnit.SECONDS));
    LOGGER.info("Result: {} successes, {} failures.", results.size(), failures.size());

    if (!failures.isEmpty()) {
      StringBuilder sb = new StringBuilder("*** Account IDs of download failures ***");
      sb.append(SEPARATOR);
      sb.append(Joiner.on(SEPARATOR).withKeyValueSeparator(": ").join(failures));
      LOGGER.error(sb.toString());
    }

    return results;
  }

  /**
   * Builds a new {@code ImmutableAdWordsSession} for the given cid.
   * 
   * @param protoSession the prototype of adwords session for building another session
   * @param cid the client customer id
   * @return an immutable adwords session for the specified cid
   */
  private ImmutableAdWordsSession buildSessionForCid(
      ImmutableAdWordsSession protoSession, Long cid) throws ValidationException {
    String cidStr = cid == null ? null : String.valueOf(cid);
    return protoSession.newBuilder().withClientCustomerId(cidStr).buildImmutable();
  }
  
  /**
   * Generates a CallableAwqlReportDownloader for downloading report in a service thread.
   */
  protected CallableAwqlReportDownloader genCallableAwqlReportDownloader(
      ImmutableAdWordsSession session, ReportDataLoader loader) {
    return new CallableAwqlReportDownloader(session, reportQuery, loader);
  }
}
