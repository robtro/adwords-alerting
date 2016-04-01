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

import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportRow;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test case for the {@link RunnableAlertRulesProcessor} class.
 */
@RunWith(JUnit4.class)
public class RunnableAlertRulesProcessorTest {
  private static final int NUMBER_OF_RULES = 2;
  private static final int NUMBER_OF_ENTRIES_IN_REPORT =
      TestEntitiesGenerator.getTestReportDataRows();
  private static final int ALERT_MESSAGE_PLACEHOLDERS_COUNT =
      TestEntitiesGenerator.getTestAlertMessagePlaceholdersCount();
  
  private ReportData mockedReport;

  private List<AlertRule> mockedAlertRules;

  private RunnableAlertRulesProcessor runnableAlertRulesProcessor;
  private RunnableAlertRulesProcessor runnableAlertRulesProcessor2;

  private AlertRule invalidAlertRule;

  private RunnableAlertRulesProcessor invalidAlertRulesProcessor;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    mockedReport = Mockito.spy(TestEntitiesGenerator.getTestReportData());

    // For alert rules processing test
    mockedAlertRules = new ArrayList<AlertRule>(NUMBER_OF_RULES);
    for (int i = 0; i < NUMBER_OF_RULES; ++i) {
      mockedAlertRules.add(Mockito.spy(TestEntitiesGenerator.getNoOpAlertRule()));
    }

    ReportData report = TestEntitiesGenerator.getTestReportData();
    String alertMessage = TestEntitiesGenerator.getTestAlertMessageTemplate();
    runnableAlertRulesProcessor =
        Mockito.spy(new RunnableAlertRulesProcessor(report, mockedAlertRules, alertMessage));

    // For alert message processing test
    runnableAlertRulesProcessor2 = Mockito.spy(
        new RunnableAlertRulesProcessor(mockedReport, new ArrayList<AlertRule>(), alertMessage));

    // For invalid alert rules processing test
    invalidAlertRule = Mockito.spy(TestEntitiesGenerator.getNoOpAlertRule());
    List<String> invalidHeaderField = Collections.singletonList(ConfigTags.ALERT_MESSAGE);
    Mockito.doReturn(invalidHeaderField).when(invalidAlertRule).newReportHeaderFields();

    invalidAlertRulesProcessor = Mockito.spy(new RunnableAlertRulesProcessor(
        report, Collections.singletonList(invalidAlertRule), alertMessage));
  }
  
  @Test
  public void testAlertRulesProcessing() {
    runnableAlertRulesProcessor.run();
    verify(runnableAlertRulesProcessor, times(1)).run();

    verify(runnableAlertRulesProcessor, times(NUMBER_OF_RULES))
        .extendReportData(Mockito.<AlertRule>anyObject(), Mockito.<ReportData>anyObject());
    verify(runnableAlertRulesProcessor, times(NUMBER_OF_RULES))
        .filterReportData(Mockito.<AlertRule>anyObject(), Mockito.<ReportData>anyObject());
    verify(runnableAlertRulesProcessor, times(1))
        .appendAlertMessages(Mockito.<ReportData>anyObject());

    for (int i = 0; i < NUMBER_OF_RULES; ++i) {
      verify(mockedAlertRules.get(i), times(1)).newReportHeaderFields();
      verify(mockedAlertRules.get(i), times(NUMBER_OF_ENTRIES_IN_REPORT))
          .appendReportEntryFields(Mockito.<ReportRow>anyObject());
      verify(mockedAlertRules.get(i), times(NUMBER_OF_ENTRIES_IN_REPORT))
          .shouldRemoveReportEntry(Mockito.<ReportRow>anyObject());
    }
  }
  
  @Test
  public void testAlertMessageProcessing() {
    runnableAlertRulesProcessor2.run();
    verify(runnableAlertRulesProcessor2, times(1)).run();

    verify(runnableAlertRulesProcessor2, times(0))
        .extendReportData(Mockito.<AlertRule>anyObject(), Mockito.<ReportData>anyObject());
    verify(runnableAlertRulesProcessor2, times(0))
        .filterReportData(Mockito.<AlertRule>anyObject(), Mockito.<ReportData>anyObject());
    verify(runnableAlertRulesProcessor2, times(1))
        .appendAlertMessages(Mockito.<ReportData>anyObject());

    int numberOfMatches = ALERT_MESSAGE_PLACEHOLDERS_COUNT * NUMBER_OF_ENTRIES_IN_REPORT;
    verify(mockedReport, times(1)).appendNewField(Mockito.anyString());
    verify(mockedReport, times(1)).getRows();
    verify(mockedReport, times(numberOfMatches)).getIndexMapping();
    verify(mockedReport, times(numberOfMatches)).getFieldIndex(Mockito.anyString());
  }
  
  @Test
  public void testInvalidAlertRulesProcessing() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(ConfigTags.ALERT_MESSAGE);

    invalidAlertRulesProcessor.run();
  }
}
