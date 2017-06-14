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
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test case for the {@link AwqlReportQuery} class.
 */
@RunWith(JUnit4.class)
public class AwqlReportQueryTest {

  /**
  * Test the AWQL query generation from JSON config.
  */
  @Test
  public void testReportQueryGeneration() {
    // Test full AWQL query
    JsonObject jsonConfig = TestEntitiesGenerator.getTestReportQueryConfig();
    AwqlReportQuery reportQuery1 = new AwqlReportQuery(jsonConfig);
    String expectedAwqlStr1 = "SELECT ExternalCustomerId,AccountDescriptiveName,Cost "
                            + "FROM ACCOUNT_PERFORMANCE_REPORT "
                            + "WHERE Impressions > 100 "
                            + "DURING THIS_MONTH";

    assertEquals(
        "Verify report type of case 1", "ACCOUNT_PERFORMANCE_REPORT", reportQuery1.getReportType());
    assertEquals("Verify AWQL query of case 1", expectedAwqlStr1, reportQuery1.generateAWQL());

    // Test AWQL query without "WHERE" clause
    jsonConfig.remove("Conditions");
    AwqlReportQuery reportQuery2 = new AwqlReportQuery(jsonConfig);
    String expectedAwqlStr2 = "SELECT ExternalCustomerId,AccountDescriptiveName,Cost "
                            + "FROM ACCOUNT_PERFORMANCE_REPORT "
                            + "DURING THIS_MONTH";

    assertEquals(
        "Verify report type of case 2", "ACCOUNT_PERFORMANCE_REPORT", reportQuery2.getReportType());
    assertEquals("Verify AWQL query of case 2", expectedAwqlStr2, reportQuery2.generateAWQL());

    // Test AWQL query without "DURING" clause
    jsonConfig.remove("DateRange");
    AwqlReportQuery reportQuery3 = new AwqlReportQuery(jsonConfig);
    String expectedAwqlStr3 = "SELECT ExternalCustomerId,AccountDescriptiveName,Cost "
                            + "FROM ACCOUNT_PERFORMANCE_REPORT";

    assertEquals(
        "Verify report type of case 3", "ACCOUNT_PERFORMANCE_REPORT", reportQuery3.getReportType());
    assertEquals("Verify AWQL query of case 3", expectedAwqlStr3, reportQuery3.generateAWQL());
  }
}
