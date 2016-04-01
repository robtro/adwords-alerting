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

import static org.junit.Assert.assertTrue;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test case for the {@link AlertAction} classes.
 */
@RunWith(JUnit4.class)
public class AlertActionsTest {
  private static final String[] ALERT_ACTION_CLASS_NAMES = {
      "com.google.api.ads.adwords.awalerting.sampleimpl.action.NoOpAlertAction",
      "com.google.api.ads.adwords.awalerting.sampleimpl.action.SimpleConsoleWriter",
      "com.google.api.ads.adwords.awalerting.sampleimpl.action.SimpleLogFileWriter",
      "com.google.api.ads.adwords.awalerting.sampleimpl.action.PerAccountManagerEmailSender",
      "com.google.api.ads.adwords.awalerting.sampleimpl.action.SQLDBPersister"};

  /**
   * Test each alert action implementation adheres to the interface definition.
   */
  @Test
  public void testAlertActionImplementations() throws Exception {
    final String assertMsg =
        "All sample alert action classes should implement AlertAction interface.";
    for (String alertActionClassName : ALERT_ACTION_CLASS_NAMES) {
      // Check that the alert action class implements AlertAction interface
      Class<?> alertActionClass = Class.forName(alertActionClassName);
      assertTrue(assertMsg, AlertAction.class.isAssignableFrom(alertActionClass));
      
      // Check that the alert action class has a construction with a JsonObject arguments
      alertActionClass.getConstructor(new Class<?>[] {JsonObject.class});
    }
  }
}
