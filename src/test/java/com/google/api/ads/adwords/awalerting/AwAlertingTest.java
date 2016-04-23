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

package com.google.api.ads.adwords.awalerting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.ads.adwords.awalerting.util.TestEntitiesGenerator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

/**
 * Test case for the {@link AwAlerting} class.
 */
@RunWith(JUnit4.class)
public class AwAlertingTest {
  /**
   * Test the file reading feature, and that the account IDs are properly added to the given set.
   */
  @Test
  public void testAddAccountFromFile() throws AlertConfigLoadException {    
    Set<Long> clientCustomerIds =
        AwAlerting.getAccountsFromStream(TestEntitiesGenerator.getTestAccountsStream());
    assertEquals(
        "Verify number of client customer IDs loaded from file.", clientCustomerIds.size(), 5);

    final String errorMsg = "Verify the specific acocunt ID is loaded.";
    assertTrue(errorMsg, clientCustomerIds.contains(1235431234L));
    assertTrue(errorMsg, clientCustomerIds.contains(3492871722L));
    assertTrue(errorMsg, clientCustomerIds.contains(5731985421L));
    assertTrue(errorMsg, clientCustomerIds.contains(3821071791L));
    assertTrue(errorMsg, clientCustomerIds.contains(5471928097L));
    assertFalse(errorMsg, clientCustomerIds.contains(5471928098L));
  }
}
