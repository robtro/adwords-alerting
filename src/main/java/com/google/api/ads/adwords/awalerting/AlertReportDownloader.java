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
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;

import java.util.List;
import java.util.Set;

/**
 * As this first step of alerting, we need to get some AdWords report data for analysis. An alert
 * report downloader will be invoked to download report data for all relevant client customer
 * accounts.
 *
 * <p>This is the interface that every alert report downloader should implement. Note that every
 * implementation MUST have a constructor with a JsonObject parameter for configuration.
 *
 */
public interface AlertReportDownloader {
  /**
   * Downloads reports and transforms them to ReportData.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param clientCustomerIds the list of client customer IDs for downloading report data
   * @return a collection of ReportData objects
   */
  List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession, Set<Long> clientCustomerIds)
      throws AlertProcessingException;
}
