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
import com.google.api.ads.adwords.awalerting.util.AdWordsServicesUtil;
import com.google.api.ads.adwords.jaxws.v201705.cm.ApiException_Exception;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionField;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionServiceInterface;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to download report definition and generate (displayFiledName -> filedName) mapping
 * for each required report type.
 */
@NotThreadSafe
public class AwReportDefinitionDownloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwReportDefinitionDownloader.class);

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
   * Generates (displayFiledName -> filedName) mapping for the specified report type.
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
   * Downloads fields from ReportDefinitionService (with retry logic) and generates
   * (displayFiledName -> filedName) mapping for the specified report type.
   *
   * @param reportType the specified report type
   */
  @VisibleForTesting
  Map<String, String> generateFieldsMapping(final ReportDefinitionReportType reportType)
      throws AlertProcessingException {
    ReportDefinitionServiceInterface reportDefinitionService =
        AdWordsServicesUtil.getService(session, ReportDefinitionServiceInterface.class);
    
    List<ReportDefinitionField> reportDefinitionFields = null;
    try {
      reportDefinitionFields = reportDefinitionService.getReportFields(reportType);
    } catch (ApiException_Exception e) {
      throw new AlertProcessingException(
          "ApiException_Exception occurred when downloading report definition.", e);
    }

    // Generate the fields mapping.
    LOGGER.info("Successfully downloaded report definition for {}.", reportType.value());
    Map<String, String> fieldsMapping = new HashMap<String, String>(reportDefinitionFields.size());
    for (ReportDefinitionField field : reportDefinitionFields) {
      fieldsMapping.put(field.getDisplayFieldName(), field.getFieldName());
    }

    return fieldsMapping;
  }
}
