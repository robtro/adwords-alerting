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

package com.google.api.ads.adwords.awalerting.processor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case for the {@link RunnableAlertActionProcessor} class.
 */
@RunWith(JUnit4.class)
public class RunnableAlertActionProcessorTest {
  private static final int NUMBER_OF_REPORTS = 10;
  private static final int NUMBER_OF_ENTRIES_IN_REPORT =
      TestEntitiesGenerator.getTestReportDataRows();

  private RunnableAlertActionProcessor runnableAlertActionProcessor;
  
  @Mock
  private AlertAction mockedAlertAction;
  
  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    List<ReportData> reports = new ArrayList<ReportData>(NUMBER_OF_REPORTS);
    for (int i = 0; i < NUMBER_OF_REPORTS; ++i) {
      reports.add(TestEntitiesGenerator.getTestReportData());
    }

    runnableAlertActionProcessor =
        Mockito.spy(new RunnableAlertActionProcessor(mockedAlertAction, reports));
  }
  
  @Test
  public void testRun() throws AlertProcessingException {
    runnableAlertActionProcessor.run();
    verify(runnableAlertActionProcessor, times(1)).run();

    verify(mockedAlertAction, times(1)).initializeAction();
    verify(mockedAlertAction, times(NUMBER_OF_REPORTS * NUMBER_OF_ENTRIES_IN_REPORT))
        .processReportEntry(Mockito.<UnmodifiableReportRow>anyObject());
    verify(mockedAlertAction, times(1)).finalizeAction();
  }
}
