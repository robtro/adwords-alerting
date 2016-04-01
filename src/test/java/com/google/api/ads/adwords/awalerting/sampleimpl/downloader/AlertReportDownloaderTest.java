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

import static org.junit.Assert.assertTrue;

import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test case for the {@link AlertReportDownloader} classes.
 */
@RunWith(JUnit4.class)
public class AlertReportDownloaderTest {
  private static final String[] ALERT_REPORT_DOWNLOADER_CLASS_NAMES = {
      "com.google.api.ads.adwords.awalerting.sampleimpl.downloader.NoOpAlertReportDownloader",
      "com.google.api.ads.adwords.awalerting.sampleimpl.downloader.AwqlReportDownloader"};

  /**
   * Test each alert report downloader implementation adheres to the interface definition.
   */
  @Test
  public void testAlertReportDownloaderImplementations() throws Exception {
    final String assertMsg = "All sample alert report downloader classes should implement"
        + " AlertReportDownloader interface.";
    for (String alertReportDownloaderClassName : ALERT_REPORT_DOWNLOADER_CLASS_NAMES) {
      // Check that the alert rule class implements AlertReportDownloader interface
      Class<?> alertReportDownloaderClass = Class.forName(alertReportDownloaderClassName);
      assertTrue(
          assertMsg, AlertReportDownloader.class.isAssignableFrom(alertReportDownloaderClass));

      // Check that the alert report downloader class has a construction with
      // an AdWordsSession.Builder and a JsonObject arguments
      alertReportDownloaderClass.getConstructor(
          new Class<?>[] {AdWordsSession.class, JsonObject.class});
    }
  }
}
