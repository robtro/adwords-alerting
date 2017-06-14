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

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.util.AdWordsServicesUtil;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.jaxb.v201705.DownloadFormat;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201705.ReportDownloaderInterface;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link Callable} implements the core logic to download the reporting data from the AdWords
 * API.
 */
public class CallableAwqlReportDownloader implements Callable<ReportData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallableAwqlReportDownloader.class);

  private final AdWordsSession session;
  private final AwqlReportQuery reportQuery;
  private final ReportDataLoader reportDataLoader;

  /**
   * The constructor takes an adwords session and an AWQL query for downloading report, and a
   * report data loader for generating ReportData from stream.
   */
  public CallableAwqlReportDownloader(
      AdWordsSession session,
      AwqlReportQuery reportQuery,
      ReportDataLoader reportDataLoader) {
    this.session = Preconditions.checkNotNull(session, "session cannot be null.");
    this.reportQuery = Preconditions.checkNotNull(reportQuery, "reportQuery cannot be null.");
    this.reportDataLoader =
        Preconditions.checkNotNull(reportDataLoader, "reportDataLoader cannot be null.");
  }
  
  /**
   * Downloads report from API (with retry logic) and transforms the result into a
   * {@link ReportData} object.
   */
  @Override
  public ReportData call() throws AlertProcessingException {
    ReportDownloaderInterface reportDownloader =
        AdWordsServicesUtil.getUtility(session, ReportDownloaderInterface.class);

    ReportDownloadResponse reportDownloadResponse = null;
    try {
      reportDownloadResponse =
          reportDownloader.downloadReport(reportQuery.generateAWQL(), DownloadFormat.GZIPPED_CSV);
    } catch (ReportException | ReportDownloadResponseException e) {
      String msg = "Failed to download report account " + session.getClientCustomerId() + ".";
      LOGGER.error(msg, e);
      throw new AlertProcessingException(msg, e);
    }
    
    InputStream inputStream = reportDownloadResponse.getInputStream();
    return handleReportStreamResult(inputStream);
  }

  /**
   * Transforms the downloaded result into a {@link ReportData} object.
   *
   * @param reportStream the downloaded report stream
   * @return the downloaded report data
   */
  private ReportData handleReportStreamResult(InputStream reportStream)
      throws AlertProcessingException {
    Preconditions.checkState(reportStream != null, "Cannot get report data: input stream is NULL.");

    // Get clientCustomerId from session and covert to Long type. The string field was set from
    // Long type in AwqlReportDownloader, so it's able to parse back to Long.
    Long clientCustomerId = Long.parseLong(session.getClientCustomerId());
    
    GZIPInputStream gzipReportStream = null;
    try {
      gzipReportStream = new GZIPInputStream(reportStream);
      
      // Parse the CSV file into report.
      LOGGER.debug("Starting processing rules of report...");
      ReportData result = reportDataLoader.fromStream(gzipReportStream, clientCustomerId);
      LOGGER.debug("... success.");
      
      gzipReportStream.close();
      return result;
    } catch (IOException e) {
      String errorMsg = String.format(
          "Error when unzipping and loading the %s of account %s from stream.",
          reportQuery.getReportType(), session.getClientCustomerId());
      LOGGER.error(errorMsg);
      throw new AlertProcessingException(errorMsg, e);
    }
  }
}
