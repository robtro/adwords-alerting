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

package com.google.api.ads.adwords.awalerting.report;

import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import com.google.gson.JsonObject;

/**
 * Generator of AWQL report query.
 * <p>Refer to AWQL guide for more info: https://developers.google.com/adwords/api/docs/guides/awql
 */
public class AwqlReportQuery {
  
  private static final class ReportQueryTags {
    public static final String REPORT_TYPE = "ReportType";
    public static final String FIELDS = "Fields";
    public static final String CONDITIONS = "Conditions";
    public static final String DATE_RANGE = "DateRange";
  }
  
  private final String reportType;
  private final String fields;
  private final String conditions;
  private final String dateRange;
  
  /**
   * @param config the JSON configuration of the report query
   */
  public AwqlReportQuery(JsonObject config) {
    reportType = config.get(ReportQueryTags.REPORT_TYPE).getAsString();
    fields = config.get(ReportQueryTags.FIELDS).getAsString();
    
    conditions =
        config.has(ReportQueryTags.CONDITIONS)
            ? config.get(ReportQueryTags.CONDITIONS).getAsString()
            : null;

    dateRange =
        config.has(ReportQueryTags.DATE_RANGE)
            ? config.get(ReportQueryTags.DATE_RANGE).getAsString()
            : null;
  }
  
  public String getReportType() {
    return reportType;
  }
  
  public ReportDefinitionReportType getReportTypeEnum() {
    return ReportDefinitionReportType.valueOf(reportType);
  }
  
  /**
   * Generates AWQL report query.
   * 
   * @return AWQL report query string
   */
  public String generateAWQL() {
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ").append(fields).append(" FROM ").append(reportType);
    
    if (conditions != null) {
      builder.append(" WHERE ").append(conditions);
    }
    
    if (dateRange != null) {
      builder.append(" DURING ").append(dateRange);
    }

    return builder.toString();
  }
}
