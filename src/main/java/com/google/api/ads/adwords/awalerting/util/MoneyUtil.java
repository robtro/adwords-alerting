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

import java.text.DecimalFormat;

/**
 * Util class for Money value conversion (from micro amount).
 */
public final class MoneyUtil {
  private static final long MICRO_MULTIPLIER = 1000000;

  private static final DecimalFormat df = new DecimalFormat("0.##");

  /**
   * Private constructor to prevent initialization.
   */
  private MoneyUtil() {}

  /**
   * Converts micro amount to currency amount.
   *
   * @param microAmount the micro amount value
   */
  public static String toCurrencyAmountStr(Long microAmount) {
    double normalAmount = (double) microAmount / MICRO_MULTIPLIER;
    return df.format(normalAmount);
  }
}
