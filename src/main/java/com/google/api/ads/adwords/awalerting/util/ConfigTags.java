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

/**
 * Helper class that defines the tags in JSON alerts configuration.
 */
public final class ConfigTags {  
  public static final String USER_AGENT_PREFIX = "AwAlerting";

  public static final String ALERTS = "Alerts";
  public static final String ALERT_NAME = "AlertName";
  public static final String REPORT_DOWNLOADER = "ReportDownloader";
  public static final String RULES = "Rules";
  public static final String ALERT_MESSAGE = "AlertMessage";
  public static final String ACTIONS = "Actions";
  public static final String CLASS_NAME = "ClassName";
  
  // Regular expression for placeholders in alert message template
  public static final String ALERT_MESSAGE_PLACEHOLDER_REGEX = "\\{\\w+\\}";
}
