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

import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportRow;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.common.base.Preconditions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This {@link Runnable} implements the core logic to apply alert rules on reports, one report
 * per thread.
 *
 * <p>The {@link List} passed to this runner is considered to be synchronized and thread safe.
 * This class has no blocking logic when adding elements to the list.
 */
public class RunnableAlertRulesProcessor implements Runnable {
  private static final Pattern alertMessagePattern =
      Pattern.compile(ConfigTags.ALERT_MESSAGE_PLACEHOLDER_REGEX);

  private CountDownLatch latch;

  private final ReportData report;
  private final List<AlertRule> rules;
  private final String alertMessage;

  /**
   * @param report the report to apply rules
   * @param rules the list of alert rules to apply
   */
  public RunnableAlertRulesProcessor(
      ReportData report, List<AlertRule> rules, String alertMessage) {
    this.report = report;
    this.rules = rules;
    this.alertMessage = alertMessage;
  }

  /**
   * Executes the API call to run alert actions on the report that was given when this
   * {@code Runnable} was created.
   *
   * <p>The processing blocks this thread until it is finished.
   */
  @Override
  public void run() {
    // The execution is in the same thread
    for (AlertRule rule : rules) {
      extendReportData(rule, report);
      filterReportData(rule, report);
    }
    appendAlertMessages(report);

    if (this.latch != null) {
      this.latch.countDown();
    }
  }

  /**
   * Extend ReportData (add more columns) using the specified alert rule.
   *
   * @param rule the AlertRule to use
   * @param report the ReportData to extend
   */
  protected void extendReportData(AlertRule rule, ReportData report) {
    List<String> reportHeaderFields = rule.newReportHeaderFields();
    if (reportHeaderFields != null) {
      for (String newHeaderField : reportHeaderFields) {
        Preconditions.checkState(!newHeaderField.equals(ConfigTags.ALERT_MESSAGE),
            "AlertRule \"%s\" cannot add a header field with name \"%s\"!",
            rule.getClass().getSimpleName(), ConfigTags.ALERT_MESSAGE);
        report.appendNewField(newHeaderField);
      }
    }

    Map<String, Integer> mapping = report.getIndexMapping();
    for (List<String> row : report.getRows()) {
      ReportRow curRow = new ReportRow(row, mapping);
      rule.appendReportEntryFields(curRow);
    }
  }

  /**
   * Filter unwanted ReportData entries using the specified alert rule.
   *
   * @param rule the AlertRule to use
   * @param report the ReportData to filter
   */
  protected void filterReportData(AlertRule rule, ReportData report) {
    Map<String, Integer> mapping = report.getIndexMapping();
    for (Iterator<List<String>> iter = report.getRows().iterator(); iter.hasNext();) {
      List<String> row = iter.next();
      ReportRow curRow = new ReportRow(row, mapping);
      if (rule.shouldRemoveReportEntry(curRow)) {
        iter.remove();
      }
    }
  }

  /**
   * Add the alert message into the report.
   *
   * @param report the ReportData to process (for each entry, add alert message column)
   */
  protected void appendAlertMessages(ReportData report) {
    report.appendNewField(ConfigTags.ALERT_MESSAGE);

    // replace all {...} placeholders to the values for each entry
    Matcher alertMessageMatcher = alertMessagePattern.matcher(alertMessage);
    for (List<String> row : report.getRows()) {
      alertMessageMatcher.reset();
      StringBuffer sb = new StringBuffer();
      while (alertMessageMatcher.find()) {
        String curMatch = alertMessageMatcher.group();

        // sanity check
        int length = curMatch.length();
        Preconditions.checkArgument(
            length > 2 && curMatch.charAt(0) == '{' && curMatch.charAt(length - 1) == '}',
            "Alert message template contains invalid placeholder: %s", curMatch);

        String fieldName = curMatch.substring(1, length - 1);
        int index = report.getFieldIndex(fieldName);
        String replacement = Matcher.quoteReplacement(row.get(index));
        alertMessageMatcher.appendReplacement(sb, replacement);
      }
      alertMessageMatcher.appendTail(sb);
      row.add(sb.toString());
    }
  }

  /**
   * @param latch the latch to set
   */
  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }
}
