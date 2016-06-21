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

package com.google.api.ads.adwords.awalerting;

import com.google.api.ads.adwords.awalerting.report.ReportRow;

import java.util.List;

/**
 * Alert rules are responsible for enriching / transforming / filtering report data that will be
 * used to fire alerts.
 *
 * <p>This is the interface that every alert rule should implement. Note that every implementation
 * MUST have a constructor with a JsonObject parameter for configuration.
 *
 * <p>Important note: All implementations MUST be thread-safe, as the instance will be shared
 * among multiple threads.
 */
public interface AlertRule {
  /**
   * Return new column names that the alert rule will extend in the report.
   */
  List<String> newReportColumns();

  /**
   * Append new field values into the report entry.
   *
   * @param entry the report entry to append new values
   */
  void appendReportEntryValues(ReportRow entry);
  
  /**
   * Modify values in the report entry.
   * @param entry the report entry to transform 
   */
  void transformReportEntry(ReportRow entry);

  /**
   * Check whether a report entry should be removed from result alerts.
   *
   * @param entry the report entry to check
   * @return whether this report entry should be removed from result alerts
   */
  boolean shouldRemoveReportEntry(ReportRow entry);
}
