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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An alert rule implementation that converts money values from micro amount to normal unit for
 * specified fields. Note that it must be thread-safe.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "ConvertMoneyValue",
 *   "MoneyField": "Cost",
 *   // "MoneyFields": ["Cost", ...],
 * }
 * </pre>
 */
public class ConvertMoneyValue implements AlertRule {
  private static final String MONEY_FIELD_TAG = "MoneyField";
  private static final String MONEY_FIELDS_TAG = "MoneyFields";
  private static final String DEFAULT_MONEY_FIELD = "Cost";

  private Set<String> moneyFields;

  public ConvertMoneyValue(JsonObject config) {
    if (config.has(MONEY_FIELD_TAG) && config.has(MONEY_FIELDS_TAG)) {
      String errorMsg = String.format(
          "Error in ConvertMoneyValue constructor: cannot have both \"%s\" and \"%s\" in config.",
          MONEY_FIELD_TAG, MONEY_FIELDS_TAG);
      throw new IllegalArgumentException(errorMsg);
    }
    
    moneyFields = new HashSet<String>();
    if (config.has(MONEY_FIELD_TAG)) {
      moneyFields.add(config.get(MONEY_FIELD_TAG).getAsString());
    } else if (config.has(MONEY_FIELDS_TAG)) {
      JsonArray array = config.get(MONEY_FIELDS_TAG).getAsJsonArray();
      for (int i = 0; i < array.size(); i++) {
        moneyFields.add(array.get(i).getAsString());
      }
    } else {
      // Use default
      moneyFields.add(DEFAULT_MONEY_FIELD);
    }
  }

  /**
   * Do not extend new columns names in the report
   */
  @Override
  public List<String> newReportColumns() {
    return null;
  }

  /**
   * Do not append new field values into report entry.
   */
  @Override
  public void appendReportEntryValues(ReportRow entry) {}
  
  /**
   * Update the money fields from micro amount to normal value
   */
  @Override
  public void transformReportEntry(ReportRow entry) {
    for (String moneyField : moneyFields) {
      long microAmount = Long.parseLong(entry.getFieldValue(moneyField));
      entry.setFieldValue(moneyField, MoneyUtil.toCurrencyAmountStr(microAmount));
    }
  }

  /**
   * Do not remove any entry from result alerts.
   */
  @Override
  public boolean shouldRemoveReportEntry(ReportRow entry) {
    return false;
  }
}
