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

import com.google.api.ads.adwords.jaxws.v201605.cm.ReportDefinitionReportType;
import com.google.common.base.Preconditions;

import au.com.bytecode.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for generating ReportData from various data sources.
 */
public class ReportDataLoader {
  private final ReportDefinitionReportType reportType;
  
  /**
   * The (displayFieldName -> fieldName) mapping.
   */
  private final Map<String, String> fieldsMapping;
  
  public ReportDataLoader(
      ReportDefinitionReportType reportType, Map<String, String> fieldsMapping) {
    this.reportType = reportType;
    this.fieldsMapping = fieldsMapping;
  }
  
  /**
   * Generate ReportData from an input stream (normally an HTTP steam of report in CSV format),
   * which will be closed after reading.
   * @param stream the input stream (in CSV format)
   * @param clientCustomerId the client customer ID of this report
   * @return the generated ReportData
   */
  public ReportData fromStream(InputStream stream, Long clientCustomerId) throws IOException {
    CSVReader csvReader = new CSVReader(new InputStreamReader(stream));
    String[] headerArray = csvReader.readNext();
    @SuppressWarnings("unchecked")  // [google3 is using low version of opencsv lib] MOE:strip_line
    List<String[]> rowsArray = csvReader.readAll();
    csvReader.close();
    
    int rowsCount = rowsArray.size();
    List<List<String>> rows = new ArrayList<List<String>>(rowsCount);
    for (int i = 0; i < rowsCount; ++i) {
      // need to create a new ArrayList object which is extendible.
      List<String> row = new ArrayList<String>(Arrays.asList(rowsArray.get(i)));
      rows.add(row);
    }

    int columns = headerArray.length;
    List<String> columnNames = new ArrayList<String>(columns);
    for (int i = 0; i < columns; i++) {
      String fieldName = fieldsMapping.get(headerArray[i]);
      Preconditions.checkNotNull(fieldName, "Unknown field name: %s.", fieldName);
      columnNames.add(fieldName);
    }
    
    return new ReportData(clientCustomerId, reportType, columnNames, rows);
  }
}
