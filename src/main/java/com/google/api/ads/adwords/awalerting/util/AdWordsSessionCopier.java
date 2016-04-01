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

import com.google.api.ads.adwords.lib.client.AdWordsSession;

/**
 * Class to copy {@code AdWordsSession} for API invocation of each AdWords account
 */
public class AdWordsSessionCopier {
  private final AdWordsSession session;

  /**
   * @param session the adwords session for the API.
   */
  public AdWordsSessionCopier(AdWordsSession session) {
    this.session = session;
  }

  /**
   * Builds a new COPY {@code AdWordsSession} for the given cid.
   *
   * @param cid the adwords account id
   * @return a copy of adwords session for the specified cid.
   */
  public AdWordsSession getAdWordsSessionCopy(Long cid) {
    AdWordsSession adWordsSession = AdWordsSessionUtil.copy(session);
    adWordsSession.setClientCustomerId(String.valueOf(cid));
    return adWordsSession;
  }
}
