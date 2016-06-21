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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201605.DetailedReportDownloadResponseException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;

/**
 * Test case for the {@link CallableAwqlReportDownloader} class.
 */
@RunWith(JUnit4.class)
public class CallableAwqlReportDownloaderTest {

  @Spy private CallableAwqlReportDownloader mockedCallableAwqlReportDownloader;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws ValidationException {
    AdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    JsonObject jsonConfig = TestEntitiesGenerator.getTestReportQueryConfig();
    AwqlReportQuery reportQuery = new AwqlReportQuery(jsonConfig);
    ReportDataLoader reportDataLoader = TestEntitiesGenerator.getTestReportDataLoader();

    mockedCallableAwqlReportDownloader =
        new CallableAwqlReportDownloader(session, reportQuery, reportDataLoader);
    mockedCallableAwqlReportDownloader.setMaxNumberOfAttempts(5);
    mockedCallableAwqlReportDownloader.setBackoffInterval(0);

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

  @Test
  public void testRun_retries() throws AlertProcessingException {
    ReportException e = new ReportException("ReportException: UnitTest Retryable Server Error");
    AlertProcessingException ex =
        new AlertProcessingException("ReportException occurs when downloading report.", e);
    doThrow(ex).when(mockedCallableAwqlReportDownloader).getReportDownloadResponse();

    thrown.expect(AlertProcessingException.class);
    thrown.expectMessage("Failed to download report after all retries.");
    mockedCallableAwqlReportDownloader.call();

    verify(mockedCallableAwqlReportDownloader, times(5)).getReportDownloadResponse();
    verify(mockedCallableAwqlReportDownloader, times(1)).call();
  }

  /**
   * Test for DetailedReportDownloadResponseExceptions, which breaks the retry logic.
   */
  @Test
  public void testRun_noRetry() throws AlertProcessingException {
    DetailedReportDownloadResponseException e =
        new DetailedReportDownloadResponseException(404, "UnitTest non-Retryable Server Error");
    AlertProcessingException ex =
        new AlertProcessingException(
            "ReportDownloadResponseException occurs when downloading report.", e);
    doThrow(ex).when(mockedCallableAwqlReportDownloader).getReportDownloadResponse();

    thrown.expect(AlertProcessingException.class);
    thrown.expectMessage(ex.getMessage());
    mockedCallableAwqlReportDownloader.call();

    verify(mockedCallableAwqlReportDownloader, times(1)).getReportDownloadResponse();
    verify(mockedCallableAwqlReportDownloader, times(1)).call();
  }
}
