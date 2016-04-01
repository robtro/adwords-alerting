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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Test case for the {@link AwqlReportDownloader} class.
 */
@RunWith(JUnit4.class)
public class AwqlReportDownloaderTest {

  private static final int NUMBER_OF_ACCOUNTS = 100;

  @Spy
  private AwqlReportDownloader mockedAwqlReportDownloader;
  
  @Mock
  private AwReportDefinitionDownloader reportDefinitionDownloader;
  
  @Before
  public void setUp() throws Exception {
    AdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    JsonObject config = TestEntitiesGenerator.getTestReportDownloaderConfig();
    mockedAwqlReportDownloader = new AwqlReportDownloader(session, config);

    MockitoAnnotations.initMocks(this);

    mockedAwqlReportDownloader.setAwReportDefinitionDownloader(reportDefinitionDownloader);
    mockReportDefinitionDownloader();
  }

  private void mockReportDefinitionDownloader() throws AlertProcessingException {
    Mockito
        .doAnswer(new Answer<Map<String, String>>() {
          @Override
          public Map<String, String> answer(InvocationOnMock invocation) throws Throwable {
            ReportDefinitionReportType reportType =
                (ReportDefinitionReportType) invocation.getArguments()[0];
            if (reportType.equals(ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT)) {
              return TestEntitiesGenerator.getTestFiledsMapping();
            }
            // Undefined report type on this test
            throw (new Exception("Undefined report type on Tests: " + reportType.value()));
          }
        })
        .when(reportDefinitionDownloader)
        .getFieldsMapping(Mockito.<ReportDefinitionReportType>anyObject());
  }
  
  /**
   * Tests the downloadReports(...).
   */
  @Test
  public void testDownloadReports() throws AlertProcessingException {
   Mockito.doAnswer(new Answer<Future<ReportData>>() {
     @Override
     public Future<ReportData> answer(InvocationOnMock invocation) throws Throwable {
       ((CountDownLatch) invocation.getArguments()[2]).countDown();
       return Futures.immediateFuture(TestEntitiesGenerator.getTestReportData());
     }
   }).when(mockedAwqlReportDownloader).submitCallableDownloader(
       Mockito.<ExecutorService>anyObject(),
       Mockito.<CallableAwqlReportDownloader>anyObject(),
       Mockito.<CountDownLatch>anyObject());
   
   Set<Long> cids = new HashSet<Long>(NUMBER_OF_ACCOUNTS);
   for (int i = 1; i <= NUMBER_OF_ACCOUNTS; i++) {
     cids.add(Long.valueOf(i));
   }
   List<ReportData> results = mockedAwqlReportDownloader.downloadReports(cids);

   ArgumentCaptor<CountDownLatch> argument = ArgumentCaptor.forClass(CountDownLatch.class);
   verify(mockedAwqlReportDownloader, times(NUMBER_OF_ACCOUNTS)).submitCallableDownloader(
       Mockito.<ExecutorService>anyObject(),
       Mockito.<CallableAwqlReportDownloader>anyObject(),
       argument.capture());
  
   assertEquals("CountDownLactch should reach 0 after all download jobs complete",
       argument.getValue().getCount(), 0);
   assertEquals("Number of reports downloaded should equal to number of accounts",
       results.size(), NUMBER_OF_ACCOUNTS);
  }
}
