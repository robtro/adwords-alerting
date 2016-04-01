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
import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Alert rules processor is responsible for processing a list of alert rules and an alert message
 * on this ReportData.
 *
 * <p>The "Rules" config for alert is optional, but "AlertMessage" config is compulsory.
 */
public class AlertRulesProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertRulesProcessor.class);

  private final int numThreads;
  private final List<AlertRule> rules;
  private String alertMessage;

  /**
   * @param configs the JSON array of alert rules configurations, could be null
   * @param alertMessage the alert message template string
   * @param numThreads the number of threads to use
   */
  public AlertRulesProcessor(JsonArray configs, String alertMessage, int numThreads) {
    this.rules = new ArrayList<AlertRule>(configs == null ? 0 : configs.size());
    this.alertMessage = alertMessage;
    this.numThreads = numThreads;

    if (configs != null) {
      for (JsonElement config : configs) {
        try {
          AlertRule rule = getRuleObject(config.getAsJsonObject());
          rules.add(rule);
        } catch (AlertConfigLoadException e) {
          // Skip this rule, and try next one
          LOGGER.error(e.toString());
        }
      }
    }
  }

  /**
   * Construct the AlertRule object according to the JSON configuration.
   *
   * @param config the JSON configuration of the alert rule
   * @return the instantiated AlertRule object
   */
  protected AlertRule getRuleObject(JsonObject config) throws AlertConfigLoadException {
    String className = config.get(ConfigTags.CLASS_NAME).getAsString();
    if (!className.contains(".")) {
      className = "com.google.api.ads.adwords.awalerting.sampleimpl.rule." + className;
    }
    
    try {
      Class<?> c = Class.forName(className);
      if (!AlertRule.class.isAssignableFrom(c)) {
        throw new InstantiationException("Wrong AlertRule class specified: " + className);
      }

      return (AlertRule) c.getConstructor(new Class<?>[] {JsonObject.class}).newInstance(config);
    } catch (Exception e) {
      throw new AlertConfigLoadException("Error constructing AlertRule with config: " + config, e);
    }
  }

  /**
   * Process the ReportData list with the alert rules, each report with all rules per thread.
   *
   * @param reports the list of ReportData to run each alert action against
   */
  public void processReports(List<ReportData> reports) throws AlertProcessingException {
    // Create one thread for each report, and apply all alert rules in sequence
    Stopwatch stopwatch = Stopwatch.createStarted();

    CountDownLatch latch = new CountDownLatch(reports.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    for (ReportData report : reports) {
      RunnableAlertRulesProcessor rulesProcessor =
          new RunnableAlertRulesProcessor(report, rules, alertMessage);
      executeRunnableAlertRulesProcessor(executorService, rulesProcessor, latch);
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AlertProcessingException("AlertRulesProcessor encounters InterruptedException.", e);
    }

    executorService.shutdown();
    stopwatch.stop();

    LOGGER.info("*** Processed {} rules and add alert messages on {} reports in {} seconds.",
        rules.size(), reports.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
  }

  protected void executeRunnableAlertRulesProcessor(ExecutorService executorService,
      RunnableAlertRulesProcessor rulesProcessor, CountDownLatch latch) {
    rulesProcessor.setLatch(latch);
    executorService.execute(rulesProcessor);
  }

  /**
   * For testing.
   */
  @VisibleForTesting
  public int getRulesCount() {
    return rules.size();
  }
}
