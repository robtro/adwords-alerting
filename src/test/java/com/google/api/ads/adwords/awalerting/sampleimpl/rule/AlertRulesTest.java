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

package com.google.api.ads.adwords.awalerting.sampleimpl.rule;

import static org.junit.Assert.assertTrue;

import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test case for the {@link AlertRule} classes.
 */
@RunWith(JUnit4.class)
public class AlertRulesTest {
  private static final String[] ALERT_RULE_CLASS_NAMES = {
      "com.google.api.ads.adwords.awalerting.sampleimpl.rule.NoOpAlertRule",
      "com.google.api.ads.adwords.awalerting.sampleimpl.rule.AddAccountManager",
      "com.google.api.ads.adwords.awalerting.sampleimpl.rule.AddAccountMonthlyBudget",
      "com.google.api.ads.adwords.awalerting.sampleimpl.rule.ConvertMoneyValue"};

  /**
   * Test each alert rule implementation adheres to the interface definition.
   */
  @Test
  public void testAlertRuleImplementations() throws Exception {
    final String assertMsg =
        "All sample alert rule classes should implement AlertRule interface.";
    for (String alertRuleClassName : ALERT_RULE_CLASS_NAMES) {
      // Check that the alert rule class implements AlertRule interface
      Class<?> alertRuleClass = Class.forName(alertRuleClassName);
      assertTrue(assertMsg, AlertRule.class.isAssignableFrom(alertRuleClass));
      
      // Check that the alert rule class has a construction with a JsonObject arguments
      alertRuleClass.getConstructor(new Class<?>[] {JsonObject.class});
    }
  }
}
