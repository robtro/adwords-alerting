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

package com.google.api.ads.adwords.awalerting.authentication;

import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.adwords.lib.client.reporting.ReportingConfiguration;
import com.google.api.ads.common.lib.auth.GoogleClientSecretsBuilder;
import com.google.api.ads.common.lib.auth.GoogleClientSecretsBuilder.Api;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Handles OAuth2 authentication for Installed application instances.
 */
@Component
public class InstalledOAuth2Authenticator implements Authenticator {
  private static final Logger LOGGER = LoggerFactory.getLogger(InstalledOAuth2Authenticator.class);

  private static final String SCOPE = "https://www.googleapis.com/auth/adwords";
  private static final String CALLBACK_URL = "urn:ietf:wg:oauth:2.0:oob";

  private final String developerToken;
  private final String clientId;
  private final String clientSecret;
  private final String managerAccountId;
  private final String userAgent;
  private String refreshToken = null;

  /**
   * Constructor with the OAuth2 parameters autowired by Spring.
   *
   * @param developerToken the developer token
   * @param clientId the OAuth2 authentication clientId
   * @param clientSecret the OAuth2 authentication clientSecret
   */
  @Autowired
  public InstalledOAuth2Authenticator(@Value(value = "${userAgent}") String userAgent,
      @Value(value = "${developerToken}") String developerToken,
      @Value(value = "${clientId}") String clientId,
      @Value(value = "${clientSecret}") String clientSecret, @Value(value = "${managerAccountId}")
      String managerAccountId,
      @Value(value = "${refreshToken}") String refreshToken) throws ValidationException {
    if (Strings.isNullOrEmpty(userAgent) || userAgent.contains(ConfigTags.USER_AGENT_PREFIX)) {
      throw new ValidationException(
          String.format(
              "User agent must be set and not be the default [%s]", ConfigTags.USER_AGENT_PREFIX),
          "userAgent");
    }
    
    this.userAgent = ConfigTags.USER_AGENT_PREFIX + "-" + userAgent;
    this.developerToken = developerToken;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.managerAccountId = managerAccountId;
    this.refreshToken = refreshToken;
  }

  /**
   * Get an immutable adwords session from the authentication info.
   */
  @Override
  public ImmutableAdWordsSession authenticate() throws OAuthException, ValidationException {
    // For easy processing, skip report header and summary (but keep column header).
    ReportingConfiguration reportingConfig =
        new ReportingConfiguration.Builder()
            .skipReportHeader(true)
            .skipColumnHeader(false)
            .skipReportSummary(true)
            .includeZeroImpressions(true)
            .build();
    
    return new AdWordsSession.Builder()
        .withOAuth2Credential(getOAuth2Credential())
        .withUserAgent(this.userAgent)
        .withClientCustomerId(this.managerAccountId)
        .withDeveloperToken(this.developerToken)
        .withReportingConfiguration(reportingConfig)
        .buildImmutable();
  }

  /**
   * Builds the OAuth 2.0 credential for the user with a known refreshToken.
   *
   * @return the new {@link Credential}
   */
  private Credential buildOAuth2Credentials() {
    return new GoogleCredential.Builder()
        .setClientSecrets(clientId, clientSecret)
        .setJsonFactory(new JacksonFactory())
        .setTransport(new NetHttpTransport())
        .build()
        .setRefreshToken(refreshToken);
  }

  /**
   * Obtains an OAuth {@link Credential} configured for AwAlerting doing the OAuth dance.
   * This method should be invoked for any users for which a refresh token is not known or is
   * invalid.
   *
   * @return The OAuth2 credential
   * @throws OAuthException an error in obtaining a token
   */
  @Override
  public Credential getOAuth2Credential() throws OAuthException {
    Credential credential = null;
    if (Strings.isNullOrEmpty(refreshToken)) {
      try {
        LOGGER.debug("Auth Token is uninitialized or forced to regen. Getting a new one.");
        credential = getNewOAuth2Credential();
      } catch (OAuthException e) {
        if (e.getMessage().contains("Connection reset")) {
          LOGGER.info("Connection reset when getting Auth Token, retrying...");
          credential = getNewOAuth2Credential();
        } else {
          LOGGER.error("Error in authentication.", e);
          throw e;
        }
      }
    } else {
      credential = buildOAuth2Credentials();
    }

    return credential;
  }

  /**
   * Get New Credentials from the user from the command line OAuth2 dance.
   */
  private Credential getNewOAuth2Credential() throws OAuthException {
    GoogleAuthorizationCodeFlow authorizationFlow = getAuthorizationFlow();
    String authorizeUrl =
        authorizationFlow.newAuthorizationUrl().setRedirectUri(CALLBACK_URL).build();

    System.out.println("\n**ACTION REQUIRED** Paste this url in your browser"
        + " and authenticate using your **AdWords Admin Email**: \n" + authorizeUrl);

    // Wait for the authorization code.
    System.out.println("\nType the code you received on the web page here: ");
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String authorizationCode = reader.readLine();

      // Authorize the OAuth2 token.
      GoogleAuthorizationCodeTokenRequest tokenRequest =
          authorizationFlow.newTokenRequest(authorizationCode);
      tokenRequest.setRedirectUri(CALLBACK_URL);
      GoogleTokenResponse tokenResponse = tokenRequest.execute();

      //  Create the credential.
      Credential credential =
          new GoogleCredential.Builder()
              .setClientSecrets(clientId, clientSecret)
              .setJsonFactory(new JacksonFactory())
              .setTransport(new NetHttpTransport())
              .build()
              .setFromTokenResponse(tokenResponse);

      // Get refresh token and prompt to save in properties file
      refreshToken = credential.getRefreshToken();
      System.out.println("\n**ACTION REQUIRED** Put the following line in your properties file to"
          + " avoid OAuth authentication next time.");
      System.out.printf("refreshToken=%s\n\n", refreshToken);

      System.out.println("Then press enter to continue...");
      reader.readLine();

      return credential;
    } catch (IOException e) {
      throw new OAuthException("An error occured obtaining the OAuth2Credential", e.getCause());
    }
  }

  private GoogleAuthorizationCodeFlow getAuthorizationFlow() {
    GoogleClientSecrets clientSecrets = null;
    try {
      clientSecrets =
          new GoogleClientSecretsBuilder()
              .forApi(Api.ADWORDS)
              .withClientSecrets(clientId, clientSecret)
              .build();
    } catch (ValidationException e) {
      System.err.println(
          "Please input your client ID and secret into your properties file, which is either "
          + "located in your home directory in your java/resources directory, or on your "
          + "classpath. If you do not have a client ID or secret, please create one in the "
          + "API console: https://code.google.com/apis/console#access");
      System.exit(1);
    }

    return new GoogleAuthorizationCodeFlow
        .Builder(
            new NetHttpTransport(),
            new JacksonFactory(),
            clientSecrets,
            Lists.newArrayList(SCOPE))
        // Set the access type to offline so that the token can be refreshed.
        // By default, the library will automatically refresh tokens when it
        // can, but this can be turned off by setting
        // api.adwords.refreshOAuth2Token=false in your ads.properties file.
        .setAccessType("offline")
        .build();
  }
}
