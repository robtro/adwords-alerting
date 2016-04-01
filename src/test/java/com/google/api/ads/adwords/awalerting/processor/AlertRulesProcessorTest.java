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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Test case for the {@link AlertRulesProcessor} class.
 */
@RunWith(JUnit4.class)
public class AlertRulesProcessorTest {
  
  private static final int NUMBER_OF_RULES = 3;
  private static final int NUMBER_OF_REPORTS = 10;
  
  @Spy
  private AlertRulesProcessor alertRulesProcessor;
  
  @Before
  public void setUp() {
    JsonObject alertRuleConfig = new JsonObject();
    alertRuleConfig.addProperty(ConfigTags.CLASS_NAME, "NoOpAlertRule");

    JsonArray configs = new JsonArray();
    for (int i = 0; i < NUMBER_OF_RULES; ++i) {
      configs.add(alertRuleConfig);
    }
    String alertMessage = TestEntitiesGenerator.getTestAlertMessageTemplate();
    alertRulesProcessor = new AlertRulesProcessor(configs, alertMessage, 10);

    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void testConstruction() {
    assertEquals("Number of alert rules should match config setting.",
        alertRulesProcessor.getRulesCount(), NUMBER_OF_RULES);
  }
  
  @Test
  public void testProcessReports() throws IOException, AlertProcessingException {
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        ((CountDownLatch) invocation.getArguments()[2]).countDown();
        return null;
      }
    }).when(alertRulesProcessor).executeRunnableAlertRulesProcessor(
        Mockito.<ExecutorService>anyObject(),
        Mockito.<RunnableAlertRulesProcessor>anyObject(),
        Mockito.<CountDownLatch>anyObject());
    
    ReportData report = TestEntitiesGenerator.getTestReportData();
    List<ReportData> reports = new ArrayList<ReportData>();
    for (int i = 0; i < NUMBER_OF_REPORTS; ++i) {
      reports.add(report);
    }
    
    alertRulesProcessor.processReports(reports);
    
    ArgumentCaptor<CountDownLatch> argument = ArgumentCaptor.forClass(CountDownLatch.class);
    verify(alertRulesProcessor, times(NUMBER_OF_REPORTS)).executeRunnableAlertRulesProcessor(
        Mockito.<ExecutorService>anyObject(),
        Mockito.<RunnableAlertRulesProcessor>anyObject(),
        argument.capture());
   
    assertEquals("CountDownLactch should reach 0 after all alert rule jobs complete",
        argument.getValue().getCount(), 0);
  }
}
