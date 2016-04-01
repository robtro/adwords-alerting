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

package com.google.api.ads.adwords.awalerting.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.sampleimpl.downloader.NoOpAlertReportDownloader;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.HashSet;
import java.util.Set;

/**
 * Test case for the {@link AlertReportDownloaderProcessor} class.
 */
@RunWith(JUnit4.class)
public class AlertReportDownloaderProcessorTest {
  @Spy
  private AlertReportDownloaderProcessor alertReportDownloaderProcessor;

  @Before
  public void setUp() throws Exception {
    AdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    JsonObject alertReportDownloaderConfig = new JsonObject();
    alertReportDownloaderConfig.addProperty(ConfigTags.CLASS_NAME, "NoOpAlertReportDownloader");

    alertReportDownloaderProcessor =
        new AlertReportDownloaderProcessor(session, alertReportDownloaderConfig);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testConstruction() {
    AlertReportDownloader alertReportDownloader =
        alertReportDownloaderProcessor.getAlertReportDownloader();

    assertNotNull("Alert report downloader should be successfully created.", alertReportDownloader);
    assertThat("This alert report downloader be a NoOpAlertReportDownloader instance",
        alertReportDownloader, instanceOf(NoOpAlertReportDownloader.class));
  }

  @Test
  public void testDownloadReports() throws AlertProcessingException {
    Set<Long> accountIds = new HashSet<Long>();
    alertReportDownloaderProcessor.downloadReports(accountIds);

    verify(alertReportDownloaderProcessor, times(1))
        .downloadReports(Mockito.<Set<Long>>anyObject());
  }
}
