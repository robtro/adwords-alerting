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
import com.google.api.ads.adwords.jaxws.factory.AdWordsServices;
import com.google.api.ads.adwords.jaxws.v201603.cm.ApiException_Exception;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionField;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionServiceInterface;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Class to download report definition and generate (displayFiledName -> filedName) mapping
 * for each required report type.
 */
@NotThreadSafe
public class AwReportDefinitionDownloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwReportDefinitionDownloader.class);

  private static final int RETRIES_COUNT = 5;
  private static final int BACKOFF_INTERVAL = 1000 * 5;

  private int retriesCount = RETRIES_COUNT;
  private int backoffInterval = BACKOFF_INTERVAL;

  private final AdWordsSession session;

  private final Map<ReportDefinitionReportType, Map<String, String>> reportFieldsMappings =
      new HashMap<ReportDefinitionReportType, Map<String, String>>();

  /**
   * @param session the adwords session
   */
  public AwReportDefinitionDownloader(AdWordsSession session) {
    this.session = session;
  }

  /**
   * Get (displayFiledName -> filedName) mapping for the specified report type.
   *
   * @param reportType the specified report type
   */
  public Map<String, String> getFieldsMapping(ReportDefinitionReportType reportType)
      throws AlertProcessingException {
    Map<String, String> fieldsMapping = reportFieldsMappings.get(reportType);
    if (fieldsMapping == null) {
      fieldsMapping = generateFieldsMapping(reportType);
      reportFieldsMappings.put(reportType, fieldsMapping);
    }

    return fieldsMapping;
  }

  /**
   * For the specified report type, Download fields from ReportDefinitionService and
   * generate (displayFiledName -> filedName) mapping.
   *
   * @param reportType the specified report type
   */
  private Map<String, String> generateFieldsMapping(ReportDefinitionReportType reportType)
      throws AlertProcessingException {
    List<ReportDefinitionField> reportDefinitionFields = null;
    for (int i = 1; i <= this.retriesCount; ++i) {
      try {
        reportDefinitionFields = downloadReportDefinitionFields(reportType);
        break;
      } catch (ApiException_Exception e) {
        LOGGER.error("(Error getting report definition: {}, cause: {}. Retry#{}/{})", e,
            e.getCause(), i, retriesCount);

        if (this.retriesCount == i) {
          // failed the last retry, just rethrow
          throw new AlertProcessingException(
              "Error getting report defintion after all retries.", e);
        }
      }

      // Slow down the rate of requests increasingly to avoid running into rate limits.
      try {
        long backoff = (long) Math.scalb(this.backoffInterval, i);
        LOGGER.error("Back off for {}ms before next retry.", backoff);
        Thread.sleep(backoff);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AlertProcessingException(
            "InterruptedException occurs while waiting to retry getting report defintion", e);
      }
    }

    // Generate the fields mapping.
    LOGGER.info("Successfully downloaded report definition for {}.", reportType.value());
    Map<String, String> fieldsMapping = new HashMap<String, String>(reportDefinitionFields.size());
    for (ReportDefinitionField field : reportDefinitionFields) {
      fieldsMapping.put(field.getDisplayFieldName(), field.getFieldName());
    }

    return fieldsMapping;
  }

  /**
   * Download the definition fields of the specified report type.
   *
   * @param reportType the specified report type
   * @return the list of report definition fields
   */
  protected List<ReportDefinitionField> downloadReportDefinitionFields(
      ReportDefinitionReportType reportType) throws ApiException_Exception {
    ReportDefinitionServiceInterface reportDefinitionService =
        new AdWordsServices().get(session, ReportDefinitionServiceInterface.class);
    return reportDefinitionService.getReportFields(reportType);
  }

  /**
   * For testing
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
