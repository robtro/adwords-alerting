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

package com.google.api.ads.adwords.awalerting.sampleimpl.action;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An alert action implementation that writes alert messages in console.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "SimpleConsoleWriter"
 * }
 * </pre>
 */
public class SimpleConsoleWriter extends AlertAction {
  public SimpleConsoleWriter(JsonObject config) {
    super(config);
  }

  /**
   * Initialization action: print some header lines.
   */
  @Override
  public void initializeAction() {
    Date now = new Date();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    System.out.println("Alerts generated at " + dateFormat.format(now) + ":");
  }

  /**
   * Process a report entry, and write its alert message in console.
   *
   * @param entry the report entry to process
   */
  @Override
  public void processReportEntry(UnmodifiableReportRow entry) {
    System.out.println(entry.getFieldValue(ConfigTags.ALERT_MESSAGE));
  }

  /**
   * Finalization action: print some foot lines.
   */
  @Override
  public void finalizeAction() {
    System.out.println();
  }
}
