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

import com.google.api.ads.adwords.awalerting.AlertConfigLoadException;
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Set;

/**
 * Alert rules processor is responsible for downloading the report data.
 */
public class AlertReportDownloaderProcessor {
  private AlertReportDownloader reportDownloader;

  /**
   * @param config the report downloader configuration
   */
  public AlertReportDownloaderProcessor(JsonObject config)
      throws AlertConfigLoadException {
    reportDownloader = getReportDownloaderObject(config);
  }

  /**
   * Construct the AlertReportDownloader object according to the JSON configuration.
   *
   * @param config the JSON configuration of the report downloader
   * @return the instantiated AlertReportDownloader object
   */
  protected AlertReportDownloader getReportDownloaderObject(JsonObject config)
      throws AlertConfigLoadException {
    String className = config.get(ConfigTags.CLASS_NAME).getAsString();
    if (!className.contains(".")) {
      className = "com.google.api.ads.adwords.awalerting.sampleimpl.downloader." + className;
    }
    
    try {
      Class<?> c = Class.forName(className);
      if (!AlertReportDownloader.class.isAssignableFrom(c)) {
        throw new InstantiationException(
            "Wrong AlertReportDownloader class specified: " + className);
      }

      return (AlertReportDownloader) c.getConstructor(new Class<?>[] {JsonObject.class})
          .newInstance(config);
    } catch (Exception e) {
      throw new AlertConfigLoadException(
          "Error constructing AlertReportDownloader with config: " + config, e);
    }
  }

  /**
   * Use the AlertReportDownloader object to download reports.
   */
  public List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession, Set<Long> clientCustomerIds)
      throws AlertProcessingException {
    return reportDownloader.downloadReports(protoSession, clientCustomerIds);
  }

  /**
   * For testing.
   */
  @VisibleForTesting
  protected AlertReportDownloader getAlertReportDownloader() {
    return reportDownloader;
  }
}
