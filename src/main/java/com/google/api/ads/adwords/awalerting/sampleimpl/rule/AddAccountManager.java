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

package com.google.api.ads.adwords.awalerting.sampleimpl.rule;

import com.google.api.ads.adwords.awalerting.AlertRule;
import com.google.api.ads.adwords.awalerting.report.ReportRow;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * An alert rule implementation that adds account manager information of the account.
 * Note that it must be thread-safe.
 * 
 * <p>The main logic for finding the account manager of an AdWords account is in function
 * getAccountManager(), which you can modify according to your business need.
 *
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "AddAccountManager"
 * }
 * </pre>
 */
public class AddAccountManager extends AlertRule {
  /**
   * Helper inner class for account manager.
   */
  class AccountManager {
    public String name;
    public String email;

    AccountManager(String name, String email) {
      this.name = name;
      this.email = email;
    }
  }

  private Random random;
  private List<AccountManager> accountManagers;

  public AddAccountManager(JsonObject config) {
    super(config);
    random = new Random();
    
    // Add sample account managers.
    accountManagers = new ArrayList<AccountManager>();
    accountManagers.add(new AccountManager("Josh G.", "josh@example.com"));
    accountManagers.add(new AccountManager("Michael F.", "michael@example.com"));
  }

  /**
   * Get account manager of the specified account.
   *
   * As a demonstration, it just randomly choose an account manager.
   *
   * @param clientCustomerId the client customer ID.
   * @return the account manager of the specified account ID.
   */
  private AccountManager getAccountManager(@SuppressWarnings("unused") String clientCustomerId) {
    Preconditions.checkState(accountManagers.size() > 0, "No account managers defined yet.");
    
    // Look up clientCustomerId and returns the corresponding account manager.
    // This is for demo only, so it just randomly assigns account to an AM. In reality you may
    // need to look up AM assignment from CRM system.
    int index = random.nextInt(accountManagers.size());
    return accountManagers.get(index);
  }

  /**
   * Extend new columns names for account manager in the report.
   */
  @Override
  public List<String> newReportColumns() {
    return Arrays.asList("AccountManagerName", "AccountManagerEmail");
  }

  /**
   * Append new field values for account manager into the report entry.
   *
   * @param entry the report entry to append new values.
   */
  @Override
  public void appendReportEntryValues(ReportRow entry) {
    String clientCustomerId = entry.getFieldValue("ExternalCustomerId");
    AccountManager am = getAccountManager(clientCustomerId);
    entry.appendFieldValues(Arrays.asList(am.name, am.email));
  }

  /**
   * Do not remove any entry from result alerts.
   */
  @Override
  public boolean shouldRemoveReportEntry(ReportRow entry) {
    return false;
  }
}
