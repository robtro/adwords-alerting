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

import static org.junit.Assert.assertEquals;

import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

/**
 * Test case for the {@link ReportData} class.
 */
@RunWith(JUnit4.class)
public class ReportDataTest {

  /**
   * Test the CSV file loading into ReportData object.
   */
  @Test
  public void testLoadReportData() throws Exception {
    ReportData report = TestEntitiesGenerator.getTestReportData();

    assertEquals("Verify the report type",
        report.getReportType(), ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT);
    
    // Check index mappings
    int idxExternalCustomerId = report.getColumnIndex("ExternalCustomerId");
    int idxDate = report.getColumnIndex("Date");
    int idxAccountDescriptiveName = report.getColumnIndex("AccountDescriptiveName");
    int idxCost = report.getColumnIndex("Cost");
    int idxClicks = report.getColumnIndex("Clicks");
    int idxImpressions = report.getColumnIndex("Impressions");
    int idxConvertedClicks = report.getColumnIndex("ConvertedClicks");
    int idxCtr = report.getColumnIndex("Ctr");
    
    final String assertMsgColumnIndex = "Verify the report column's index";
    assertEquals(assertMsgColumnIndex, idxExternalCustomerId, 0);
    assertEquals(assertMsgColumnIndex, idxDate, 1);
    assertEquals(assertMsgColumnIndex, idxAccountDescriptiveName, 2);
    assertEquals(assertMsgColumnIndex, idxCost, 3);
    assertEquals(assertMsgColumnIndex, idxClicks, 4);
    assertEquals(assertMsgColumnIndex, idxImpressions, 5);
    assertEquals(assertMsgColumnIndex, idxConvertedClicks, 6);
    assertEquals(assertMsgColumnIndex, idxCtr, 7);
    
    int rowsCount = report.getRows().size();
    assertEquals("Verify number of rows in report", rowsCount, 7);
    
    // Check first entry
    List<String> firstRow = report.getRow(0);
    final String assertMsgFirstRow = "Verify data in the first row";
    assertEquals(assertMsgFirstRow, firstRow.get(idxExternalCustomerId), "1232198123");
    assertEquals(assertMsgFirstRow, firstRow.get(idxDate), "2013-05-01");
    assertEquals(assertMsgFirstRow, firstRow.get(idxAccountDescriptiveName), "Le Test");
    assertEquals(assertMsgFirstRow, firstRow.get(idxCost), "1420000");
    assertEquals(assertMsgFirstRow, firstRow.get(idxClicks), "10");
    assertEquals(assertMsgFirstRow, firstRow.get(idxImpressions), "1978");
    assertEquals(assertMsgFirstRow, firstRow.get(idxConvertedClicks), "0");
    assertEquals(assertMsgFirstRow, firstRow.get(idxCtr), "0.51%");

    // Check last entry
    List<String> lastRow = report.getRow(rowsCount - 1);
    final String assertMsgLastRow = "Verify data in the last row";
    assertEquals(assertMsgLastRow, lastRow.get(idxExternalCustomerId), "1232198123");
    assertEquals(assertMsgLastRow, lastRow.get(idxDate), "2013-05-10");
    assertEquals(assertMsgLastRow, lastRow.get(idxAccountDescriptiveName), "Le Test");
    assertEquals(assertMsgLastRow, lastRow.get(idxCost), "750000");
    assertEquals(assertMsgLastRow, lastRow.get(idxClicks), "4");
    assertEquals(assertMsgLastRow, lastRow.get(idxImpressions), "2793");
    assertEquals(assertMsgLastRow, lastRow.get(idxConvertedClicks), "0");
    assertEquals(assertMsgLastRow, lastRow.get(idxCtr), "0.14%");
  }
}
