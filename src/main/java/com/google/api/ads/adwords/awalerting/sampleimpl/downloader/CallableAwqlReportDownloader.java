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
import com.google.api.ads.adwords.awalerting.util.RetryHelper;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.jaxb.v201605.DownloadFormat;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201605.DetailedReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.v201605.ReportDownloader;
import com.google.api.client.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

/**
 * This {@link Callable} implements the core logic to download the reporting data from the AdWords
 * API.
 */
public class CallableAwqlReportDownloader implements Callable<ReportData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallableAwqlReportDownloader.class);

  private static final int MAX_NUMBER_OF_ATTEMPTS = 20;
  private static final int BACKOFF_INTERVAL = 1000 * 5;

  private int maxNumberOfAttempts = MAX_NUMBER_OF_ATTEMPTS;
  private int backoffInterval = BACKOFF_INTERVAL;

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
    // Retry on downloading report.
    Callable<ReportDownloadResponse> callable = new Callable<ReportDownloadResponse>() {
      @Override
      public ReportDownloadResponse call() throws AlertProcessingException {
        return getReportDownloadResponse();
      }
    };

    ImmutableList<Class<? extends Exception>> nonRetriableExceptions =
        ImmutableList.<Class<? extends Exception>>of(DetailedReportDownloadResponseException.class);
    ReportDownloadResponse reportDownloadResponse = RetryHelper.callsWithRetries(
        callable, "download report", maxNumberOfAttempts, backoffInterval, nonRetriableExceptions);
    
    InputStream inputStream = reportDownloadResponse.getInputStream();
    return handleReportStreamResult(inputStream);
  }

  /**
   * Gets the report download response from the API.
   */
  protected ReportDownloadResponse getReportDownloadResponse() throws AlertProcessingException {
    try {
      ReportDownloader reportDownloader = new ReportDownloader(session);
      return reportDownloader.downloadReport(
          reportQuery.generateAWQL(), DownloadFormat.GZIPPED_CSV);
    } catch (ReportException e) {
      throw new AlertProcessingException("ReportException occurs when downloading report.", e);
    } catch (ReportDownloadResponseException e) {
      throw new AlertProcessingException(
          "ReportDownloadResponseException occurs when downloading report.", e);
    }
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
      LOGGER.error(
          "Error when unzipping and loading the {} of account {}.",
          reportQuery.getReportType(), session.getClientCustomerId());
      throw new AlertProcessingException("Failed to load report data from stream.", e);
    }
  }
  
  @VisibleForTesting
  protected void setMaxNumberOfAttempts(int maxNumberOfAttempts) {
    this.maxNumberOfAttempts = maxNumberOfAttempts;
  }

  @VisibleForTesting
  protected void setBackoffInterval(int backoffInterval) {
    this.backoffInterval = backoffInterval;
  }
}
