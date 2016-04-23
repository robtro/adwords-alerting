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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.jaxws.v201603.cm.ApiException_Exception;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionField;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;

/**
 * Test case for the {@link AwReportDefinitionDownloader} class.
 */
@RunWith(JUnit4.class)
public class AwReportDefinitionDownloaderTest {
  @Spy
  private AwReportDefinitionDownloader mockedAwReportDefinitionDownloader;

  @Before
  public void setUp() throws ValidationException {
    AdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    
    mockedAwReportDefinitionDownloader = new AwReportDefinitionDownloader(session);
    mockedAwReportDefinitionDownloader.setRetriesCount(5);
    mockedAwReportDefinitionDownloader.setBackoffInterval(0);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetFieldsMapping() throws ApiException_Exception, AlertProcessingException {
    doReturn(new ArrayList<ReportDefinitionField>())
        .when(mockedAwReportDefinitionDownloader)
        .downloadReportDefinitionFields(Mockito.<ReportDefinitionReportType>anyObject());

    ReportDefinitionReportType reportType = ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT;
    mockedAwReportDefinitionDownloader.getFieldsMapping(reportType);

    verify(mockedAwReportDefinitionDownloader, times(1)).downloadReportDefinitionFields(reportType);
    verify(mockedAwReportDefinitionDownloader, times(1)).getFieldsMapping(reportType);
  }

  @Test
  public void testGetFieldsMapping_retries()
      throws ApiException_Exception, AlertProcessingException {
    ApiException_Exception ex = new ApiException_Exception(
        "ApiException", new com.google.api.ads.adwords.jaxws.v201603.cm.ApiException());
    doThrow(ex)
        .when(mockedAwReportDefinitionDownloader)
        .downloadReportDefinitionFields(Mockito.<ReportDefinitionReportType>anyObject());

    ReportDefinitionReportType reportType = ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT;
    try {
      mockedAwReportDefinitionDownloader.getFieldsMapping(reportType);
    } catch (AlertProcessingException e) {
      // do nothing
    }

    verify(mockedAwReportDefinitionDownloader, times(5)).downloadReportDefinitionFields(reportType);
    verify(mockedAwReportDefinitionDownloader, times(1)).getFieldsMapping(reportType);
  }
}
