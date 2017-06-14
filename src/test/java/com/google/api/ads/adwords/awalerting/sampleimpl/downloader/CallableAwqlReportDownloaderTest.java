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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * Test case for the {@link CallableAwqlReportDownloader} class.
 */
@RunWith(JUnit4.class)
public class CallableAwqlReportDownloaderTest {

  @Spy private CallableAwqlReportDownloader mockedCallableAwqlReportDownloader;

  @Before
  public void setUp() throws ValidationException {
    AdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    JsonObject jsonConfig = TestEntitiesGenerator.getTestReportQueryConfig();
    AwqlReportQuery reportQuery = new AwqlReportQuery(jsonConfig);
    ReportDataLoader reportDataLoader = TestEntitiesGenerator.getTestReportDataLoader();

    mockedCallableAwqlReportDownloader =
        new CallableAwqlReportDownloader(session, reportQuery, reportDataLoader);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testRun() throws AlertProcessingException, IOException {
    doReturn(TestEntitiesGenerator.getTestReportData())
        .when(mockedCallableAwqlReportDownloader)
        .call();

    mockedCallableAwqlReportDownloader.call();
    verify(mockedCallableAwqlReportDownloader, times(1)).call();
  }
}
