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

import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.client.util.Joiner;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Report data corresponding to a downloaded report for one account.
 */
public class ReportData {
  static final String SEPARATOR = System.getProperty("line.separator");
  
  private ReportDefinitionReportType reportType;

  // use List instead of native array[], since report data will be enriched.
  private List<String> header;
  private List<List<String>> rows;

  // Field name -> index mapping
  private Map<String, Integer> indexMapping;
  
  public ReportData(
      ReportDefinitionReportType reportType,
      List<String> header,
      List<List<String>> rows,
      Map<String, Integer> indexMapping) {
    this.reportType = reportType;
    this.header = header;
    this.rows = rows;
    this.indexMapping = indexMapping;
  }

  public List<String> getHeader() {
    return header;
  }

  public List<List<String>> getRows() {
    return rows;
  }

  /**
   * Get the i-th row of this report.
   *
   * @param index the index of the returning row
   * @return the i-th row of this report
   */
  public List<String> getRow(int index) {
    return rows.get(index);
  }

  public Map<String, Integer> getIndexMapping() {
    return indexMapping;
  }

  /**
   * Get the column index of the specified column name.
   *
   * @param columnName the column name
   * @return the index of the specified column
   */
  public int getFieldIndex(String columnName) {
    return indexMapping.get(columnName).intValue();
  }

  /**
   * Append a new column in the report.
   *
   * @param fieldName the name of the new field
   */
  public void appendNewField(String fieldName) {
    Preconditions.checkState(
        indexMapping.size() == header.size(),
        "Report indexMapping and header should have same size.");

    if (!indexMapping.containsKey(fieldName)) {
      int newIndex = header.size();
      header.add(fieldName);
      indexMapping.put(fieldName, Integer.valueOf(newIndex));
    }
  }

  public ReportDefinitionReportType getReportType() {
    return reportType;
  }

  /**
   * Returns string representation of the report.
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Joiner joiner = Joiner.on(',');
    
    builder.append(reportType.value()).append(":").append(SEPARATOR);
    builder.append("Header:").append(SEPARATOR).append(joiner.join(header)).append(SEPARATOR);
    
    builder.append("Data:").append(SEPARATOR);
    for (List<String> row : rows) {
      builder.append(joiner.join(row)).append(SEPARATOR);
    }
    
    return builder.toString();
  }
}
