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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Report data corresponding to a downloaded report for one account.
 */
public class ReportData {
  static final String SEPARATOR = System.getProperty("line.separator");
  
  private final ReportDefinitionReportType reportType;

  // use List instead of native array[], since report data will be enriched.
  private final List<String> header;
  private final List<List<String>> rows;

  // Field name -> row index (0-based) mapping.
  private final Map<String, Integer> indexMapping;
  
  public ReportData(
      ReportDefinitionReportType reportType,
      List<String> header,
      List<List<String>> rows,
      Map<String, Integer> indexMapping) {
    Preconditions.checkArgument(
        indexMapping.size() == header.size(),
        "Report indexMapping and header should have same size.");
    
    this.reportType = reportType;
    this.header = header;
    this.rows = rows;
    this.indexMapping = indexMapping;
  }
  
  public ReportData(
      ReportDefinitionReportType reportType,
      List<String> header,
      Map<String, Integer> indexMapping) {
    Preconditions.checkArgument(
        indexMapping.size() == header.size(),
        "Report indexMapping and header should have same size.");
    
    this.reportType = reportType;
    this.header = header;
    this.rows = new ArrayList<List<String>>();
    this.indexMapping = indexMapping;
  }

  public List<String> getHeader() {
    return header;
  }

  public List<List<String>> getRows() {
    return rows;
  }

  /**
   * Get the report row of the specified index (0-based).
   *
   * @param index the 0-based index of the returning row
   */
  public List<String> getRow(int index) {
    return rows.get(index);
  }
  
  /**
   * Add one row into the rows list.
   * @param row the row to be added
   */
  public void addRow(List<String> row) {
    rows.add(row);
  }
  
  /**
   * Get the mapping of field name -> row index (0-based).
   */
  public Map<String, Integer> getIndexMapping() {
    return indexMapping;
  }

  /**
   * Get the 0-based column index of the specified column name.
   *
   * @param columnName the column name for looking up its 0-based index
   */
  public int getFieldIndex(String columnName) {
    Preconditions.checkArgument(indexMapping.containsKey(columnName),
        "The specified column name is not available in the report: %s", columnName);
    return indexMapping.get(columnName).intValue();
  }

  /**
   * Append a new column in the report.
   *
   * @param fieldName the name of the new field
   * @throws IllegalArgumentException if the specified field name already exists in report header
   */
  public void appendNewField(String fieldName) {
    Preconditions.checkArgument(
        !indexMapping.containsKey(fieldName),
        "Cannot append new field: the field name \"%s\" already exists in the report header!",
        fieldName);
    
    int newIndex = header.size();
    header.add(fieldName);
    indexMapping.put(fieldName, Integer.valueOf(newIndex));
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
