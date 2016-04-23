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

import com.google.api.ads.adwords.awalerting.authentication.Authenticator;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test case for the {@link AlertProcessor} class.
 */
@RunWith(JUnit4.class)
public class AlertProcessorTest {
      
  @Mock
  private Authenticator authenticator;
  
  @Spy
  private AlertProcessor alertProcessor;
  
  @Captor
  ArgumentCaptor<List<ReportData>> reportsCaptor;
  
  @Before
  public void setUp() throws OAuthException, ValidationException {
    alertProcessor = new AlertProcessor(10);

    MockitoAnnotations.initMocks(this);

    // Mocking the Authentication because in OAuth2 we are force to call buildOAuth2Credentials
    ImmutableAdWordsSession session = TestEntitiesGenerator.getTestAdWordsSession();
    Mockito.doReturn(session).when(authenticator).authenticate();

    alertProcessor.setAuthentication(authenticator);
  }
  
  @Test
  public void testGenerateAlerts() throws Exception {
    InputStreamReader alertsConfigReader =
        new InputStreamReader(TestEntitiesGenerator.getTestAlertsConfigStream());
    
    int numberOfAlerts = 0;
    try {
      JsonObject alertsConfig = new JsonParser().parse(alertsConfigReader).getAsJsonObject();
      numberOfAlerts = alertsConfig.getAsJsonArray(ConfigTags.ALERTS).size();
      Set<Long> cids = new HashSet<Long>();
      alertProcessor.generateAlerts(cids, alertsConfig);
    } finally {
      alertsConfigReader.close();
    }
    
    verify(alertProcessor, times(numberOfAlerts)).processAlert(
        Mockito.<Set<Long>>anyObject(),
        Mockito.<ImmutableAdWordsSession>anyObject(),
        Mockito.<JsonObject>anyObject(),
        Mockito.anyInt());
    
    verify(alertProcessor, times(numberOfAlerts)).downloadReports(
        Mockito.<ImmutableAdWordsSession>anyObject(),
        Mockito.<Set<Long>>anyObject(),
        Mockito.<JsonObject>anyObject());
    
    verify(alertProcessor, times(numberOfAlerts)).processReports(
        reportsCaptor.capture(),
        Mockito.<JsonArray>anyObject(),
        Mockito.anyString(),
        Mockito.<JsonArray>anyObject());
  }
}
