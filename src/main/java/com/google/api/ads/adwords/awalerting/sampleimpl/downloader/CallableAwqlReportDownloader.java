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
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.jaxb.v201603.DownloadFormat;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201603.DetailedReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.v201603.ReportDownloader;
import com.google.api.client.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;

/**
 * This {@link Callable} implements the core logic to download the reporting data
 * from the AdWords API.
 */
public class CallableAwqlReportDownloader implements Callable<ReportData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CallableAwqlReportDownloader.class);

  private static final int RETRIES_COUNT = 20;
  private static final int BACKOFF_INTERVAL = 1000 * 5;

  private int retriesCount = RETRIES_COUNT;
  private int backoffInterval = BACKOFF_INTERVAL;

  private CountDownLatch latch;

  private final AdWordsSession session;
  private final AwqlReportQuery reportQuery;
  private final ReportDataLoader reportDataLoader;

  /**
   * @param session the adwords session used for downloading report
   * @param reportQuery the AWQL query to download report
   * @param reportDataLoader the loader for generating ReportData from stream
   */
  public CallableAwqlReportDownloader(
      AdWordsSession session, AwqlReportQuery reportQuery, ReportDataLoader reportDataLoader) {
    this.session = session;
    this.reportQuery = reportQuery;
    this.reportDataLoader = reportDataLoader;
  }

  /**
   * Executes the API call to download the report that was given when this
   * {@code Runnable} was created.
   *
   * <p>The download blocks this thread until it is finished, and also does the
   * file unzipping.
   *
   * <p>There is also a retry logic implemented by this method, where the times
   * retried depends on the value given in the constructor.
   */
  @Override
  public ReportData call() throws AlertProcessingException {
    ReportData result = null;
    Exception lastException = null;
    
    try {
      for (int i = 1; i <= this.retriesCount; i++) {
        try {
          lastException = null;
          result = this.downloadAndProcessReport();
          break;
        } catch (AlertProcessingException e) {
          lastException = e;
          // Retry unless it's an DetailedReportDownloadResponseException.
          if (e.getCause() instanceof DetailedReportDownloadResponseException) {
            DetailedReportDownloadResponseException detailedException =
                (DetailedReportDownloadResponseException) e.getCause();
            LOGGER.error("(Error: {}, Trigger: {})", detailedException.getType(),
                detailedException.getTrigger());
            throw e;
          }
          
          printRetryMessageOnException(e, i);
        }

        // If we haven't succeeded, slow down the rate of requests (with exponential back off)
        // to avoid running into rate limits.
        try {
          // Sleep unless this was the last attempt.
          if (i < retriesCount) {
            long backoff = (long) Math.scalb(this.backoffInterval, i);
            Thread.sleep(backoff);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AlertProcessingException(
              "InterruptedException occurs while waiting to retry downloading report", e);
        }
      }
    } finally {
      if (this.latch != null) {
        this.latch.countDown();
      }
    }
    
    if (result == null) {
      throw new AlertProcessingException(
          "Failed to download report after all retries.", lastException);
    }
    
    return result;
  }

  /**
   * Print default retry message.
   *
   * @param e the exception caught
   * @param retrySequence the sequence number of the retry
   */
  private void printRetryMessageOnException(Exception e, int retrySequence) {
    LOGGER.error("(Error downloading report: {}, Cause: {}. Retry# {}/{}.)", e, e.getCause(),
        retrySequence, retriesCount);
  }

  /**
   * Downloads the report from the API.
   *
   * @return the downloaded report data
   */
  protected ReportData downloadAndProcessReport() throws AlertProcessingException {
    try {
      ReportDownloader reportDownloader = new ReportDownloader(session);
      ReportDownloadResponse reportDownloadResponse =
          reportDownloader.downloadReport(reportQuery.generateAWQL(), DownloadFormat.GZIPPED_CSV);

      if (reportDownloadResponse.getHttpStatus() == HttpURLConnection.HTTP_OK) {
        InputStream reportStream = reportDownloadResponse.getInputStream();
        return handleReportStreamResult(reportStream);
      } else {
        LOGGER.error("getHttpStatus(): {}", reportDownloadResponse.getHttpStatus());
        LOGGER.error(
            "getHttpResponseMessage(): {}", reportDownloadResponse.getHttpResponseMessage());
        throw new AlertProcessingException("Failed to download report: HTTP status error.");
      }
    } catch (ReportException e) {
      throw new AlertProcessingException("ReportException occurs when downloading report.", e);
    } catch (ReportDownloadResponseException e) {
      throw new AlertProcessingException(
          "ReportDownloadResponseException occurs when downloading report.", e);
    }
  }

  /**
   * @param reportStream the downloaded report stream
   * @return the downloaded report data
   */
  private ReportData handleReportStreamResult(InputStream reportStream)
      throws AlertProcessingException {
    Preconditions.checkState(reportStream != null, "Cannot get report data: input stream is NULL.");
    
    try {
      GZIPInputStream gzipReportStream = new GZIPInputStream(reportStream);
      ReportData result = loadReportData(gzipReportStream);
      gzipReportStream.close();
      return result;
    } catch (IOException e) {
      LOGGER.error("Error when unzipping and loading the {} of account {}.",
          reportQuery.getReportType(), session.getClientCustomerId());
      throw new AlertProcessingException("Failed to load report data from stream.", e);
    }
  }

  /**
   * Load the report data from downloaded report stream.
   *
   * @param gzipReportStream the downloaded and unzipped stream (CSV format)
   * @return the downloaded report data
   */
  private ReportData loadReportData(GZIPInputStream gzipReportStream) throws IOException {
    // Parse the CSV file into report.
    LOGGER.debug("Starting processing rules of report...");
    ReportData result = reportDataLoader.fromStream(gzipReportStream);
    LOGGER.debug("... success.");

    return result;
  }

  /**
   * @param latch the latch to set
   */
  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }

  /**
   * For testing.
   */
  @VisibleForTesting
  protected void setRetriesCount(int retriesCount) {
    this.retriesCount = retriesCount;
  }

  @VisibleForTesting
  protected void setBackoffInterval(int backoffInterval) {
    this.backoffInterval = backoffInterval;
  }
}
