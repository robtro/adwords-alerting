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
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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

    assertEquals(
        "Verify the report type",
        ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT,
        report.getReportType());

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
    assertEquals(assertMsgColumnIndex, 0, idxExternalCustomerId);
    assertEquals(assertMsgColumnIndex, 1, idxDate);
    assertEquals(assertMsgColumnIndex, 2, idxAccountDescriptiveName);
    assertEquals(assertMsgColumnIndex, 3, idxCost);
    assertEquals(assertMsgColumnIndex, 4, idxClicks);
    assertEquals(assertMsgColumnIndex, 5, idxImpressions);
    assertEquals(assertMsgColumnIndex, 6, idxConvertedClicks);
    assertEquals(assertMsgColumnIndex, 7, idxCtr);

    int rowsCount = report.getRows().size();
    assertEquals("Verify number of rows in report", 7, rowsCount);

    // Check first entry
    List<String> firstRow = report.getRow(0);
    final String assertMsgFirstRow = "Verify data in the first row";
    assertEquals(assertMsgFirstRow, "1232198123", firstRow.get(idxExternalCustomerId));
    assertEquals(assertMsgFirstRow, "2013-05-01", firstRow.get(idxDate));
    assertEquals(assertMsgFirstRow, "Le Test", firstRow.get(idxAccountDescriptiveName));
    assertEquals(assertMsgFirstRow, "1420000", firstRow.get(idxCost));
    assertEquals(assertMsgFirstRow, "10", firstRow.get(idxClicks));
    assertEquals(assertMsgFirstRow, "1978", firstRow.get(idxImpressions));
    assertEquals(assertMsgFirstRow, "0", firstRow.get(idxConvertedClicks));
    assertEquals(assertMsgFirstRow, "0.51%", firstRow.get(idxCtr));

    // Check last entry
    List<String> lastRow = report.getRow(rowsCount - 1);
    final String assertMsgLastRow = "Verify data in the last row";
    assertEquals(assertMsgLastRow, "1232198123", lastRow.get(idxExternalCustomerId));
    assertEquals(assertMsgLastRow, "2013-05-10", lastRow.get(idxDate));
    assertEquals(assertMsgLastRow, "Le Test", lastRow.get(idxAccountDescriptiveName));
    assertEquals(assertMsgLastRow, "750000", lastRow.get(idxCost));
    assertEquals(assertMsgLastRow, "4", lastRow.get(idxClicks));
    assertEquals(assertMsgLastRow, "2793", lastRow.get(idxImpressions));
    assertEquals(assertMsgLastRow, "0", lastRow.get(idxConvertedClicks));
    assertEquals(assertMsgLastRow, "0.14%", lastRow.get(idxCtr));
  }
}
