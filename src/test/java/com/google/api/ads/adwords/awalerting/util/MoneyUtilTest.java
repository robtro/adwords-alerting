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
    assertEquals(assertMsg, "0", MoneyUtil.toCurrencyAmountStr(0L));

    assertEquals(assertMsg, "0", MoneyUtil.toCurrencyAmountStr(1000L));
    assertEquals(assertMsg, "0", MoneyUtil.toCurrencyAmountStr(4999L));
    assertEquals(assertMsg, "0.01", MoneyUtil.toCurrencyAmountStr(5001L));

    assertEquals(assertMsg, "0.01", MoneyUtil.toCurrencyAmountStr(10000L));
    assertEquals(assertMsg, "0.01", MoneyUtil.toCurrencyAmountStr(14999L));
    assertEquals(assertMsg, "0.02", MoneyUtil.toCurrencyAmountStr(15001L));

    assertEquals(assertMsg, "0.1", MoneyUtil.toCurrencyAmountStr(100000L));
    assertEquals(assertMsg, "0.11", MoneyUtil.toCurrencyAmountStr(105001L));

    assertEquals(assertMsg, "10", MoneyUtil.toCurrencyAmountStr(10000000L));
    assertEquals(assertMsg, "10.1", MoneyUtil.toCurrencyAmountStr(10100000L));
    assertEquals(assertMsg, "10.11", MoneyUtil.toCurrencyAmountStr(10110000L));
    assertEquals(assertMsg, "10.11", MoneyUtil.toCurrencyAmountStr(10111000L));
  }
}
