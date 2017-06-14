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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
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

/**
 * Test case for the {@link AlertActionsProcessor} class.
 */
@RunWith(JUnit4.class)
public class AlertActionsProcessorTest {
  
  private static final int NUMBER_OF_ACTIONS = 3;
  
  @Spy
  private AlertActionsProcessor alertActionsProcessor;
  
  @Before
  public void setUp() {
    JsonObject alertActionConfig = new JsonObject();
    alertActionConfig.addProperty(ConfigTags.CLASS_NAME, "NoOpAlertAction");

    JsonArray configs = new JsonArray();
    for (int i = 0; i < NUMBER_OF_ACTIONS; ++i) {
      configs.add(alertActionConfig);
    }
    alertActionsProcessor = new AlertActionsProcessor(configs, 10);

    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void testConstruction() {
    assertEquals(
        "Number of alert actions should match config setting.",
        NUMBER_OF_ACTIONS,
        alertActionsProcessor.getActionsSize());
  }
  
  @Test
  public void testProcessReports() throws AlertProcessingException {
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        ((CountDownLatch) invocation.getArguments()[2]).countDown();
        return null;
      }
    }).when(alertActionsProcessor).executeRunnableAlertActionProcessor(
        Mockito.<ExecutorService>anyObject(),
        Mockito.<RunnableAlertActionProcessor>anyObject(),
        Mockito.<CountDownLatch>anyObject());
    
    List<ReportData> reports = new ArrayList<ReportData>();
    alertActionsProcessor.processReports(reports);
    
    ArgumentCaptor<CountDownLatch> argument = ArgumentCaptor.forClass(CountDownLatch.class);
    verify(alertActionsProcessor, times(NUMBER_OF_ACTIONS)).executeRunnableAlertActionProcessor(
        Mockito.<ExecutorService>anyObject(),
        Mockito.<RunnableAlertActionProcessor>anyObject(),
        argument.capture());

    assertEquals(
        "CountDownLactch should reach 0 after all alert action jobs complete",
        0,
        argument.getValue().getCount());
  }
}
