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

import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.report.ReportRow;
import com.google.api.ads.adwords.awalerting.util.MoneyUtil;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * An alert rule implementation that adds account monthly budget information of the account.
 * Note that it must be thread-safe.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "AddAccountMonthlyBudget"
 * }
 * </pre>
 */
public class AddAccountMonthlyBudget extends AlertRule {
  private Random random;
  
  public AddAccountMonthlyBudget(JsonObject config) {
    super(config);
    random = new Random();
  }

  /**
   * Extend new columns name for account monthly budget in the report.
   */
  @Override
  public List<String> newReportHeaderFields() {
    return Collections.singletonList("AccountMonthlyBudget");
  }

  /**
   * Append new field value for account monthly budget into the report entry.
   *
   * As a demonstration, it just randomly chooses a monthly budget between 0 / 50 /100 dollars,
   * with 0 meaning unlimited.
   *
   * @param entry the report entry to append new field values.
   */
  @Override
  public void appendReportEntryFields(ReportRow entry) {
    // Randomly choose a monthly budget of 0, 50 or 100 dollars per month.
    int multiplier = random.nextInt(3);
    long budget = multiplier * 50; 
    entry.appendFieldValue(String.valueOf(budget));

    // Convert cost from micro amount to normal amount.
    long cost = Long.parseLong(entry.getFieldValue("Cost"));
    entry.setFieldValue("Cost", MoneyUtil.toCurrencyAmountStr(cost));
  }

  /**
   * Do not alert for accounts with budgets being well-utilized.
   */
  @Override
  public boolean shouldRemoveReportEntry(ReportRow entry) {
    double budget = Double.parseDouble(entry.getFieldValue("AccountMonthlyBudget"));

    // If budget is unlimited, don't alert.
    if (0 == budget) {
      return true;
    }

    // If it's in the first 3 days of the month, don't alert
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    int day = cal.get(Calendar.DAY_OF_MONTH);
    if (day <= 3) {
      return true;
    }

    // If average daily spend is more than 60% of available budget, don't alert
    double cost = Double.parseDouble(entry.getFieldValue("Cost"));
    int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

    if (cost / day > 0.6 * budget / daysInMonth) {
      return true;
    }

    // All other cases, fire alert
    return false;
  }
}
