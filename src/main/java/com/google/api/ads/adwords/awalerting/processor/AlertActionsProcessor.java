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

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.AlertConfigLoadException;
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
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
 * Alert actions processor is responsible for processing the list of alert actions on all the
 * ReportData objects. It will spawn a thread for each alert action, which runs on all ReportData
 * objects because some times it need to get aggregate stats from all reports.
 *
 * <p>The list of ReportData is shared among multiple threads, so it MUST NOT alter any ReportData
 * object.
 */
public class AlertActionsProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertActionsProcessor.class);

  private final int numThreads;
  private final List<AlertAction> actions;

  /**
   * @param configs the JSON array of alert actions configurations
   * @param numThreads the number of threads to use
   */
  public AlertActionsProcessor(JsonArray configs, int numThreads) {
    this.numThreads = numThreads;

    actions = new ArrayList<AlertAction>(configs.size());
    for (JsonElement config : configs) {
      try {
        AlertAction action = getActionObject(config.getAsJsonObject());
        actions.add(action);
      } catch (Exception e) {
        LOGGER.error("Error constructing alert action.", e);
        LOGGER.error("Problemetic config: {}", config);
        throw new IllegalArgumentException("Wrong AlertAction config.", e);
      }
    }
  }

  /**
   * Construct the AlertAction object according to the JSON configuration.
   *
   * @param config the JSON configuration of the alert action
   * @return the instantiated AlertAction object
   */
  protected AlertAction getActionObject(JsonObject config) throws AlertConfigLoadException {
    String className = config.get(ConfigTags.CLASS_NAME).getAsString();
    if (!className.contains(".")) {
      className = "com.google.api.ads.adwords.awalerting.sampleimpl.action." + className;
    }

    try {
      Class<?> c = Class.forName(className);
      if (!AlertAction.class.isAssignableFrom(c)) {
        throw new InstantiationException("Wrong AlertAction class specified: " + className);
      }

      return (AlertAction) c.getConstructor(new Class<?>[] {JsonObject.class}).newInstance(config);
    } catch (Exception e) {
      throw new AlertConfigLoadException(
          "Error constructing AlertAction with config: " + config, e);
    }
  }

  /**
   * Process the ReportData list with alert actions, all reports with each action per thread.
   *
   * @param reports the list of ReportData to run each alert action against.
   */
  public void processReports(List<ReportData> reports) throws AlertProcessingException {
    // Create one thread for each AlertAction, and process all reports
    Stopwatch stopwatch = Stopwatch.createStarted();

    CountDownLatch latch = new CountDownLatch(actions.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    for (AlertAction action : actions) {
      RunnableAlertActionProcessor actionProcessor =
          new RunnableAlertActionProcessor(action, reports);
      executeRunnableAlertActionProcessor(executorService, actionProcessor, latch);
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AlertProcessingException(
          "AlertActionsProcessor encounters InterruptedException.", e);
    }

    executorService.shutdown();
    stopwatch.stop();

    LOGGER.info("*** Processed {} actions on {} reports in {} seconds.", actions.size(),
        reports.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
  }

  protected void executeRunnableAlertActionProcessor(ExecutorService executorService,
      RunnableAlertActionProcessor actionProcessor, CountDownLatch latch) {
    actionProcessor.setLatch(latch);
    executorService.execute(actionProcessor);
  }

  /**
   * For testing.
   */
  @VisibleForTesting
  protected int getActionsSize() {
    return actions.size();
  }
}
