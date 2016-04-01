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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test case for the {@link MoneyUtil} classes.
 */
@RunWith(JUnit4.class)
public class MoneyUtilTest {
  /**
   * Test the conversion on micro amount.
   */
  @Test
  public void testMicroAmountConversions() {
    final String assertMsg = "Verify micro amount to currency amount conversion.";
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(0L), "0");

    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(1000L), "0");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(4999L), "0");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(5001L), "0.01");

    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(10000L), "0.01");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(14999L), "0.01");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(15001L), "0.02");

    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(100000L), "0.1");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(105001L), "0.11");

    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(10000000L), "10");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(10100000L), "10.1");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(10110000L), "10.11");
    assertEquals(assertMsg, MoneyUtil.toCurrencyAmountStr(10111000L), "10.11");
  }
}
