// Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.api.ads.adwords.extension.ratelimiter.AdWordsServicesWithRateLimiter;
import com.google.api.ads.adwords.jaxws.factory.AdWordsServices;
import com.google.api.ads.adwords.lib.client.AdWordsSession;

/**
 * Utilities based on {@link AdWordsServicesWithRateLimiter}.
 */
public class AdWordsServicesUtil {
  private static final AdWordsServicesWithRateLimiter instance =
      new AdWordsServicesWithRateLimiter(AdWordsServices.getInstance());

  private AdWordsServicesUtil() {}

  /**
   * Creates a new specific service using the {@link AdWordsServicesWithRateLimiter}.
   *
   * @param session the adwords session
   * @param interfaceClass the interface of the service
   * @return the newly created service
   */
  public static <T> T getService(AdWordsSession session, Class<T> interfaceClass) {
    return instance.get(session, interfaceClass);
  }

  /**
   * Creates a new specific utility using the {@link AdWordsServicesWithRateLimiter}.
   *
   * @param session the adwords session
   * @param utilityClass the interface of the utility
   * @return the newly created utility
   */
  public static <T> T getUtility(AdWordsSession session, Class<T> utilityClass) {
    return instance.getUtility(session, utilityClass);
  }
}
