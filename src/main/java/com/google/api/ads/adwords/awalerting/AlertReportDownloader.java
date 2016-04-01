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

package com.google.api.ads.adwords.awalerting;

import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Set;

/**
 * This is the abstract class that every alert report downloader should inherit from.
 *
 * <p>Important note: All implementations MUST have a constructor with an AdWordsSession and a
 * JsonConfig parameters, otherwise it will fail to load.
 */
public abstract class AlertReportDownloader {
  /**
   * Constructor.
   *
   * @param session the adwords session
   * @param config the JsonObject for the alert report downloader configuration
   */
  public AlertReportDownloader(AdWordsSession session, JsonObject config) {}
  
  /**
   * Download reports and process them to ReportData.
   *
   * @return a collection of ReportData objects
   */
  public abstract List<ReportData> downloadReports(Set<Long> accountIds)
      throws AlertProcessingException;
}
