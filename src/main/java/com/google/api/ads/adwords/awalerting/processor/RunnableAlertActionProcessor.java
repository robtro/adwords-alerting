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
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This {@link Runnable} implements the core logic to run alert actions on reports, one action
 * per thread.
 *
 * <p>The {@link List} passed to this runner is considered to be thread safe and won't be modified.
 */
public class RunnableAlertActionProcessor implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RunnableAlertActionProcessor.class);

  private CountDownLatch latch;

  private final AlertAction action;
  private final List<ReportData> reports;

  /**
   * @param action the AlertAction to use
   * @param reports the list of ReportData to apply the action
   */
  public RunnableAlertActionProcessor(AlertAction action, List<ReportData> reports) {
    this.action = action;
    this.reports = reports;
  }


  /**
   * Executes the API call to run alert actions on the report that was given when this
   * {@code Runnable} was created.
   *
   * <p>The processing blocks this thread until it is finished.
   */
  @Override
  public void run() {
    try {
      LOGGER.debug("Start running AlertAction \"{}\" for {} reports",
          action.getClass().getSimpleName(), reports.size());

      // Run alert action on each report
      action.initializeAction();
      for (ReportData report : reports) {
        Map<String, Integer> mapping = report.getIndexMapping();
        for (List<String> row : report.getRows()) {
          UnmodifiableReportRow curRow = new UnmodifiableReportRow(row, mapping);
          action.processReportEntry(curRow);
        }
      }
      action.finalizeAction();

      LOGGER.debug("... success.");
    } catch (AlertProcessingException e) {
      LOGGER.error("Error running AlertAction \"{}\": {}.", action.getClass().getSimpleName(), e);
    } finally {
      if (this.latch != null) {
        this.latch.countDown();
      }
    }
  }

  /**
   * @param latch the latch to set
   */
  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }
}
