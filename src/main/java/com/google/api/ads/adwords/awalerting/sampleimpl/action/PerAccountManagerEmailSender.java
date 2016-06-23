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

package com.google.api.ads.adwords.awalerting.sampleimpl.action;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An alert action implementation that creates alert emails, one for each account manager.
 * 
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ActionClass": "PerAccountManagerEmailSender",
 *   "Subject": "Low impression accounts",
 *   "CC": "abc@example.com,xyz@example.com"
 * }
 * </pre>
 */
public class PerAccountManagerEmailSender implements AlertAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerAccountManagerEmailSender.class);

  private static final String NEWLINE = String.format("%n");
  private static final String FROM = "aw-report-alerting@example.com";

  private static final String SUBJECT_TAG = "Subject";
  private static final String CC_TAG = "CC";
  
  // Each email for one receiver (account manager)
  private Map<String, AlertEmail> emailsMap;
  private String subject;
  private List<String> ccList;
  
  public PerAccountManagerEmailSender(JsonObject config) {
    subject = config.get(SUBJECT_TAG).getAsString();
    ccList = null;
    if (config.has(CC_TAG)) {
      ccList = Arrays.asList(config.get(CC_TAG).getAsString().split(","));
    }

    emailsMap = new HashMap<String, AlertEmail>();
  }

  /**
   * Initialization action: nothing to do.
   */
  @Override
  public void initializeAction() {}

  /**
   * Process a report entry, and put the alert message in the corresponding alert email object.
   * 
   * @param entry the report entry to process
   */
  @Override
  public void processReportEntry(UnmodifiableReportRow entry) {
    String to = entry.getFieldValue("AccountManagerEmail");
    AlertEmail email = emailsMap.get(to);
    if (email == null) {
      email = new AlertEmail(to);
      emailsMap.put(to, email);
    }

    String clientCustomerId = entry.getFieldValue("ExternalCustomerId");
    String alertMessage = entry.getFieldValue("AlertMessage");
    email.addAlert(clientCustomerId, alertMessage);
  }

  /**
   * Finalization action: print out the alert emails.
   */
  @Override
  public void finalizeAction() {
    StringBuffer sb = new StringBuffer();
    if (!emailsMap.isEmpty()) {
      for (AlertEmail email : emailsMap.values()) {
        sb.append(email.print(subject, FROM, ccList));
      }
    }
    LOGGER.info(sb.toString());
  }
  
  /**
   * Helper inner class for alert email.
   */
  private static class AlertEmail {
    public String to;
    public Multimap<String, String> alertsMap; // account ID -> alert messages

    public AlertEmail(String to) {
      this.to = to;
      this.alertsMap = ArrayListMultimap.create();
    }

    /**
     * Add alert message to the corresponding alert email.
     *
     * @param clientCustomerId the client customer ID
     * @param alertMessage the alert message
     */
    public void addAlert(String clientCustomerId, String alertMessage) {
      alertsMap.put(clientCustomerId, alertMessage);
    }

    /**
     * For demonstration, just prints out the email properly.
     *
     * @param subject email subject
     * @param from sender address
     * @param ccList list of cc addresses
     * @return formatted email printout
     */
    public StringBuffer print(String subject, String from, List<String> ccList) {
      // TODO(zhuoc): use template to format the email message.
      
      StringBuffer sb = new StringBuffer(NEWLINE);
      sb.append("===== Alert email starts =====");
      sb.append(NEWLINE);
      sb.append(NEWLINE);

      sb.append("From: ").append(from).append(NEWLINE);
      sb.append("To: ").append(to).append(NEWLINE);
      sb.append("Subject: ").append(subject).append(NEWLINE);

      if (ccList != null && !ccList.isEmpty()) {
        boolean firstCc = true;
        for (String cc : ccList) {
          if (firstCc) {
            sb.append("Cc: ");
            firstCc = false;
          } else {
            sb.append("    ");
          }
          sb.append(cc);
          sb.append(NEWLINE);
        }
      }

      Date now = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      sb.append("Date: " + dateFormat.format(now));
      sb.append(NEWLINE);
      sb.append(NEWLINE);

      for (String clientCustomerId : alertsMap.keySet()) {
        Collection<String> alertMessages = alertsMap.get(clientCustomerId);
        sb.append("Account ID: " + clientCustomerId);
        sb.append(NEWLINE);
        for (String alertMessage : alertMessages) {
          sb.append("  " + alertMessage);
          sb.append(NEWLINE);
        }
        sb.append(NEWLINE);
      }

      sb.append("===== Alert email ends =====");
      sb.append(NEWLINE);
      return sb;
    }
  }
}
