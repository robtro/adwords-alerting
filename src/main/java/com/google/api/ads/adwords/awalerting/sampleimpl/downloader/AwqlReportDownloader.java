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
import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.report.AwqlReportQuery;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.util.AdWordsSessionCopier;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Class to handle all the concurrent file downloads from the reporting API.
 * Note that it must have a constructor that takes an AdWordsSession and a JsonObject.
 *
 * <p>An {@link ExecutorService} is created in order to handle all the threads. To initialize the
 * executor is necessary to call {@code initializeExecutorService}, and to finalize the executor
 * is necessary to call {@code finalizeExecutorService} after all the downloads are done, and the
 * downloader will not be used again.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "AwqlReportDownloader",
 *   "ReportQuery": {
 *     "ReportType": "...",
 *     "Fields": "...",
 *     "Conditions": "...",
 *     "DateRange": "..."
 *   }
 * }
 * </pre>
 */
public class AwqlReportDownloader extends AlertReportDownloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwqlReportDownloader.class);

  private static final String REPORT_QUERY_TAG = "ReportQuery";

  private static final int NUM_THREADS = 20;
  private int numThreads = NUM_THREADS;

  private AwqlReportQuery reportQuery;
  private AdWordsSessionCopier sessionBuilderSynchronizer;
  private AwReportDefinitionDownloader reportDefinitionDownloader;

  private class DownloadJob {
    final Long accountId;
    final Future<ReportData> future;

    DownloadJob(Long accountId, Future<ReportData> future) {
      this.accountId = accountId;
      this.future = future;
    }

    public Long getAccountId() {
      return accountId;
    }

    public Future<ReportData> getFuture() {
      return future;
    }
  }

  /**
   * @param session the adwords session
   * @param config the JsonObject for this alert report downloader configuration
   */
  public AwqlReportDownloader(AdWordsSession session, JsonObject config) {
    super(session, config);
    
    this.sessionBuilderSynchronizer = new AdWordsSessionCopier(session);
    this.reportDefinitionDownloader = new AwReportDefinitionDownloader(session);
    
    JsonObject reportQueryConfig = config.getAsJsonObject(REPORT_QUERY_TAG);
    this.reportQuery = new AwqlReportQuery(reportQueryConfig);
  }

  /**
   * Downloads the specified report for all specified CIDs. Prints out list of failed CIDs.
   * Returns List<File> for all successful downloads.
   *
   * @param accountIds CIDs to download the report for
   * @return Collection of File objects reports have been downloaded to
   */
  @Override
  public List<ReportData> downloadReports(Set<Long> accountIds)
      throws AlertProcessingException {
    CountDownLatch latch = new CountDownLatch(accountIds.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    Stopwatch stopwatch = Stopwatch.createStarted();

    LOGGER.info("Downloading {} reports...", reportQuery.getReportType());
    
    Map<String, String> fieldsMapping =
        reportDefinitionDownloader.getFieldsMapping(reportQuery.getReportTypeEnum());
    ReportDataLoader loader = new ReportDataLoader(reportQuery.getReportTypeEnum(), fieldsMapping);

    List<DownloadJob> downloadJobs = new ArrayList<DownloadJob>(accountIds.size());
    for (Long accountId : accountIds) {
      // We create a copy of the AdWordsSession specific for the Account.
      AdWordsSession adWordsSession = sessionBuilderSynchronizer.getAdWordsSessionCopy(accountId);
      CallableAwqlReportDownloader callableDownloader =
          new CallableAwqlReportDownloader(adWordsSession, accountId, reportQuery, loader);
      Future<ReportData> future =
          submitCallableDownloader(executorService, callableDownloader, latch);
      downloadJobs.add(new DownloadJob(accountId, future));
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AlertProcessingException(
          "AwqlReportDownloader encounters InterruptedException.", e);
    }

    List<ReportData> results = new ArrayList<ReportData>();
    List<Long> failures = new ArrayList<Long>();
    ReportData report = null;
    for (DownloadJob job : downloadJobs) {
      try {
        report = job.getFuture().get();
        if (report == null) {
          failures.add(job.getAccountId());
        } else {
          results.add(report);
        }
      } catch (ExecutionException e) {
        failures.add(job.getAccountId());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AlertProcessingException(
            "AwqlReportDownloader encounters InterruptedException.", e);
      }
    }

    executorService.shutdown();
    stopwatch.stop();

    LOGGER.info("Downloaded reports for {} accounts in {} seconds.", accountIds.size(),
        stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
    LOGGER.info("Result: {} successes, {} failures.", results.size(), failures.size());

    if (!failures.isEmpty()) {
      LOGGER.error("*** Account IDs of download failures:");
      for (Long failure : failures) {
        LOGGER.error("\t{}", failure);
      }
    }

    return results;
  }

  protected Future<ReportData> submitCallableDownloader(ExecutorService executorService,
      CallableAwqlReportDownloader callableDownloader, CountDownLatch latch) {
    callableDownloader.setLatch(latch);
    return executorService.submit(callableDownloader);
  }

  /**
   * For Mockito testing.
   */
  @VisibleForTesting
  protected void setAwReportDefinitionDownloader(
      AwReportDefinitionDownloader reportDefinitionDownloader) {
    this.reportDefinitionDownloader = reportDefinitionDownloader;
  }
}
