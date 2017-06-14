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

package com.google.api.ads.adwords.awalerting.sampleimpl.downloader;

import static org.junit.Assert.assertEquals;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test case for the {@link AwqlReportDownloader} class. */
@RunWith(JUnit4.class)
public class AwqlReportDownloaderTest {

  private static final int NUMBER_OF_ACCOUNTS = 100;

  @Test
  public void testDownloadReports()
      throws ValidationException, AlertProcessingException, IOException {
    JsonObject config = TestEntitiesGenerator.getTestReportDownloaderConfig();
    ImmutableAdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();

    final Map<String, String> fieldsMapping = TestEntitiesGenerator.getTestFiledsMapping();
    final ReportData reportData = TestEntitiesGenerator.getTestReportData();
    final AwqlReportQuery reportQuery =
        new AwqlReportQuery(TestEntitiesGenerator.getTestReportQueryConfig());

    // Create a test AwReportDefinitionDownloader that just returns test fields mapping.
    AwReportDefinitionDownloader reportDefDownloader = new AwReportDefinitionDownloader(session) {
      @Override
      public Map<String, String> getFieldsMapping(ReportDefinitionReportType reportType) {
        return fieldsMapping;
      }
    };

    // Create a test AwqlReportDownloader which spawns test CallableAwqlReportDownloader instance,
    // which in turn just return test ReportData.
    AwqlReportDownloader reportDownloader = new AwqlReportDownloader(config) {
      @Override
      protected CallableAwqlReportDownloader genCallableAwqlReportDownloader(
          ImmutableAdWordsSession session, ReportDataLoader loader) {
        return new CallableAwqlReportDownloader(session, reportQuery, loader) {
          @Override
          public ReportData call() throws AlertProcessingException {
            System.out.println(
                "Running CallableAwqlReportDownloader on tid " + Thread.currentThread().getId());
            return reportData;
          }
        };
      }
    };

    Set<Long> cids = new HashSet<Long>(NUMBER_OF_ACCOUNTS);
    for (int i = 1; i <= NUMBER_OF_ACCOUNTS; i++) {
      cids.add(Long.valueOf(i));
    }

    List<ReportData> results = reportDownloader.downloadReports(session, reportDefDownloader, cids);

    // Check test result.
    assertEquals(
        "Number of reports downloaded should equal to number of accounts",
        NUMBER_OF_ACCOUNTS,
        results.size());

    for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++) {
      assertEquals(
          "The downloaded report data #" + (i + 1) + " is not expected",
          results.get(i), reportData);
    }
  }
}
