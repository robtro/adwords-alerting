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

package com.google.api.ads.adwords.awalerting.util;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.authentication.Authenticator;
import com.google.api.ads.adwords.awalerting.authentication.InstalledOAuth2Authenticator;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.report.ReportDataLoader;
import com.google.api.ads.adwords.awalerting.sampleimpl.action.NoOpAlertAction;
import com.google.api.ads.adwords.awalerting.sampleimpl.rule.NoOpAlertRule;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generate test entities for various test cases.
 */
public class TestEntitiesGenerator {
  public static Authenticator getTestAuthenticator() throws ValidationException {
    return new InstalledOAuth2Authenticator("UserAgent", "DevToken", "ClientId", "ClientSecret",
                                            "ManagerAccountId", "RefreshToken");
  }
  
  public static AdWordsSession.Builder getTestAdWordsSessionBuilder() {
    return new AdWordsSession.Builder()
               .withEndpoint("http://www.google.com")
               .withDeveloperToken("DeveloperToken")
               .withClientCustomerId("123")
               .withUserAgent("UserAgent")
               .withOAuth2Credential(new GoogleCredential.Builder().build());
  }
  
  public static ImmutableAdWordsSession getTestAdWordsSession() throws ValidationException {
    return getTestAdWordsSessionBuilder().buildImmutable();
  }
  
  public static JsonObject getTestReportDownloaderConfig() {
    String jsonConfigStr = "{"
                         + "  \"ClassName\": \"AwqlReportDownloader\", "
                         + "  \"ReportQuery\": {"
                         + "    \"ReportType\": \"ACCOUNT_PERFORMANCE_REPORT\", "
                         + "    \"Fields\": \"ExternalCustomerId,AccountDescriptiveName,Cost\", "
                         + "    \"Conditions\": \"Impressions > 100\", "
                         + "    \"DateRange\": \"THIS_MONTH\" "
                         + "  }"
                         + "}";
    return new JsonParser().parse(jsonConfigStr).getAsJsonObject();
  }
  
  public static JsonObject getTestReportQueryConfig() {
    return getTestReportDownloaderConfig().getAsJsonObject("ReportQuery");
  }
  
  public static String getTestAlertMessageTemplate() {
    return "Account \"{AccountDescriptiveName}\" (ID \"{ExternalCustomerId}\") has "
        + "{Impressions} impressions and {Clicks} clicks.";
  }
  
  public static int getTestAlertMessagePlaceholdersCount() {
    Pattern pattern = Pattern.compile(ConfigTags.ALERT_MESSAGE_PLACEHOLDER_REGEX);
    Matcher matcher = pattern.matcher(getTestAlertMessageTemplate());

    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
  
  public static Map<String, String> getTestFiledsMapping() {
    Map<String, String> fieldsMapping = new HashMap<String, String>();
    fieldsMapping.put("Account", "AccountDescriptiveName");
    fieldsMapping.put("Clicks", "Clicks");
    fieldsMapping.put("Cost", "Cost");
    fieldsMapping.put("Converted clicks", "ConvertedClicks");
    fieldsMapping.put("CTR", "Ctr");
    fieldsMapping.put("Day", "Date");
    fieldsMapping.put("Customer ID", "ExternalCustomerId");
    fieldsMapping.put("Impressions", "Impressions");

    return fieldsMapping;
  }
  
  public static InputStream getTestAccountsStream() {
    return TestEntitiesGenerator.class.getResourceAsStream(
        "resources/accounts-for-test.txt");
  }

  public static InputStream getTestReportStream() {
    return TestEntitiesGenerator.class.getResourceAsStream(
        "resources/reportDownload-ACCOUNT_PERFORMANCE_REPORT-2602198216-1370030134500.report");
  }

  public static InputStream getTestAlertsConfigStream() {
    return TestEntitiesGenerator.class.getResourceAsStream(
        "resources/aw-alerting-alerts-test.json");
  }

  public static ReportDataLoader getTestReportDataLoader() {
    return new ReportDataLoader(
        ReportDefinitionReportType.ACCOUNT_PERFORMANCE_REPORT, getTestFiledsMapping());
  }
  
  /**
   * Test report headers: ExternalCustomerId, Date, AccountDescriptiveName, Cost, Clicks,
   *                      Impressions, ConvertedClicks, Ctr
   * Test report entries: 7
   */
  public static ReportData getTestReportData() throws IOException {
    return getTestReportDataLoader().fromStream(getTestReportStream());
  }
  
  public static int getTestReportDataRows() {
    try {
      return getTestReportData().getRows().size();
    } catch (IOException e) {
      return -1;
    }
  }
  
  public static AlertRule getNoOpAlertRule() {
    JsonObject config = new JsonObject();
    config.addProperty(ConfigTags.CLASS_NAME, "NoOpAlertRule");
    return new NoOpAlertRule(config);
  }

  public static AlertAction getNoOpAlertAction() {
    JsonObject config = new JsonObject();
    config.addProperty(ConfigTags.CLASS_NAME, "NoOpAlertAction");
    return new NoOpAlertAction(config);
  }
}
