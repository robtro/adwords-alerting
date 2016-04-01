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
import com.google.api.ads.adwords.lib.client.AdWordsSession.Builder;
import com.google.api.ads.adwords.lib.client.reporting.ReportingConfiguration;
import com.google.api.ads.common.lib.exception.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to copy the {@link AdWordsSession}.
 */
public class AdWordsSessionUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdWordsSessionUtil.class);

  /**
   * Creates a copy of the AdWordsSession.
   *
   * @param adWordsSession to copy from
   * @return a new copy of the AdWordsSession
   */
  public static AdWordsSession copy(AdWordsSession adWordsSession) {
    return copy(adWordsSession, adWordsSession.getUserAgent());
  }

  /**
   * Creates a copy of the AdWordsSession and changes the userAgent.
   *
   * @param adWordsSession to copy from
   * @param userAgent the new User Agent for the session
   * @return a new copy of the AdWordsSession
   */
  public static AdWordsSession copy(AdWordsSession adWordsSession, String userAgent) {
    AdWordsSession.Builder builder = new Builder();
    builder.withUserAgent(userAgent != null ? userAgent : ConfigTags.USER_AGENT_PREFIX);

    if (adWordsSession.getEndpoint() != null) {
      builder.withEndpoint(adWordsSession.getEndpoint());
    }
    if (adWordsSession.getDeveloperToken() != null) {
      builder.withDeveloperToken(adWordsSession.getDeveloperToken());
    }
    if (adWordsSession.getClientCustomerId() != null) {
      builder.withClientCustomerId(adWordsSession.getClientCustomerId());
    }
    if (adWordsSession.getOAuth2Credential() != null) {
      builder.withOAuth2Credential(adWordsSession.getOAuth2Credential());
    }

    if (adWordsSession.getReportingConfiguration() != null) {
      ReportingConfiguration reportingConfig =
          new ReportingConfiguration.Builder()
              .skipReportHeader(adWordsSession.getReportingConfiguration().isSkipReportHeader())
              .skipColumnHeader(adWordsSession.getReportingConfiguration().isSkipColumnHeader())
              .skipReportSummary(adWordsSession.getReportingConfiguration().isSkipReportSummary())
              .includeZeroImpressions(
                  adWordsSession.getReportingConfiguration().isIncludeZeroImpressions())
              .build();
      builder.withReportingConfiguration(reportingConfig);
    }

    try {
      AdWordsSession newAdWordsSession = builder.build();
      newAdWordsSession.setPartialFailure(adWordsSession.isPartialFailure());
      newAdWordsSession.setValidateOnly(adWordsSession.isValidateOnly());
      return newAdWordsSession;
    } catch (ValidationException e) {
      LOGGER.warn("Error @addUtilityUserAgent, returning unchanged AdWordsSession");
      return adWordsSession;
    }
  }
}
