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
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;
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

  private final AwqlReportQuery reportQuery;

  private static class DownloadJob {
    final Long clientCustomerId;
    final Future<ReportData> future;

    DownloadJob(Long clientCustomerId, Future<ReportData> future) {
      this.clientCustomerId = clientCustomerId;
      this.future = future;
    }

    public Long getClientCustomerId() {
      return clientCustomerId;
    }

    public Future<ReportData> getFuture() {
      return future;
    }
  }

  /**
   * @param config the JsonObject for this alert report downloader configuration
   */
  public AwqlReportDownloader(JsonObject config) {
    super(config);

    JsonObject reportQueryConfig = config.getAsJsonObject(REPORT_QUERY_TAG);
    this.reportQuery = new AwqlReportQuery(reportQueryConfig);
  }

  /**
   * Downloads the specified report for all specified CIDs.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param clientCustomerIds the client customer IDs to download the report for
   * @return Collection of File objects reports have been downloaded to
   */
  @Override
  public List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession, Set<Long> clientCustomerIds)
      throws AlertProcessingException {
    ImmutableAdWordsSession session = null;
    try {
      // No need to specify clientCustomerId for getting report definition.
      session = buildSessionForCid(protoSession, null);
    } catch (ValidationException e) {
      throw new AlertProcessingException(
          "Failed to create valid adwords session for report defintion downloader.", e);
    }

    AwReportDefinitionDownloader reportDefinitionDownloader =
        new AwReportDefinitionDownloader(session);
    return downloadReports(protoSession, reportDefinitionDownloader, clientCustomerIds);
  }

    /**
   * Downloads the specified report for all specified CIDs.
   * Returns List<File> for all successful downloads, and prints out list of failed CIDs.
   *
   * @param protoSession the prototype adwords session used for downloading reports
   * @param reportDefinitionDownloader report definition downloader (for getting fields mapping)
   * @param clientCustomerIds the client customer IDs to download the report for
   * @return Collection of File objects reports have been downloaded to
   */
  protected List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession,
      AwReportDefinitionDownloader reportDefinitionDownloader,
      Set<Long> clientCustomerIds)
      throws AlertProcessingException {
    CountDownLatch latch = new CountDownLatch(clientCustomerIds.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    Stopwatch stopwatch = Stopwatch.createStarted();

    LOGGER.info("Downloading {} reports...", reportQuery.getReportType());
    
    Map<String, String> fieldsMapping =
        reportDefinitionDownloader.getFieldsMapping(reportQuery.getReportTypeEnum());
    ReportDataLoader loader = new ReportDataLoader(reportQuery.getReportTypeEnum(), fieldsMapping);
    
    List<DownloadJob> downloadJobs = new ArrayList<DownloadJob>(clientCustomerIds.size());
    List<Long> failures = new ArrayList<Long>();
    ImmutableAdWordsSession session = null;
    for (Long clientCustomerId : clientCustomerIds) {
      try {
        session = buildSessionForCid(protoSession, clientCustomerId);
      } catch (ValidationException e) {
        LOGGER.error(
            "Failed to create valid adwords session for CID {}, skipping it.", clientCustomerId);
        failures.add(clientCustomerId);
        continue;
      }
      
      CallableAwqlReportDownloader callableDownloader =
          new CallableAwqlReportDownloader(session, reportQuery, loader);
      Future<ReportData> future =
          submitCallableDownloader(executorService, callableDownloader, latch);
      downloadJobs.add(new DownloadJob(clientCustomerId, future));
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AlertProcessingException(
          "AwqlReportDownloader encounters InterruptedException.", e);
    }

    List<ReportData> results = new ArrayList<ReportData>();
    ReportData report = null;
    for (DownloadJob job : downloadJobs) {
      try {
        report = job.getFuture().get();
        results.add(report);
      } catch (ExecutionException e) {
        failures.add(job.getClientCustomerId());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AlertProcessingException(
            "AwqlReportDownloader encounters InterruptedException.", e);
      }
    }

    executorService.shutdown();
    stopwatch.stop();

    LOGGER.info("Downloaded reports for {} accounts in {} seconds.",
        clientCustomerIds.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000);
    LOGGER.info("Result: {} successes, {} failures.", results.size(), failures.size());

    if (!failures.isEmpty()) {
      LOGGER.error("*** Account IDs of download failures:");
      for (Long failure : failures) {
        LOGGER.error("\t{}", failure);
      }
    }

    return results;
  }

  /**
   * Build a new {@code ImmutableAdWordsSession} for the given cid.
   * @param protoSession the prototype of adwords session for building another session
   * @param cid the client customer id
   * @return an immutable adwords session for the specified cid
   */
  private ImmutableAdWordsSession buildSessionForCid(
      ImmutableAdWordsSession protoSession, Long cid) throws ValidationException {
    String cidStr = cid == null ? null : String.valueOf(cid);
    return protoSession.newBuilder().withClientCustomerId(cidStr).buildImmutable();
  }

  protected Future<ReportData> submitCallableDownloader(ExecutorService executorService,
      CallableAwqlReportDownloader callableDownloader, CountDownLatch latch) {
    callableDownloader.setLatch(latch);
    return executorService.submit(callableDownloader);
  }
}
