# AwAlerting Framework

## Overview

AwAlerting is an alerting framework based on AdWords reports from [AWQL]
(https://developers.google.com/adwords/api/docs/guides/awql), with defined
abstract classes for report downloader, alert rules and actions, where users
can easily implement customized logic to plug in. This tool can be used by
novice users (simple alerts through configuration) as well as more advanced
users (custom alerts through extension).

## Quick Start

### Prerequisites

You will need Java, Maven and MySQL installed before configuring the project.

### Build the project using Maven

AwAlerting can be compiled using Maven by executing the following command:

<code>$ mvn compile dependency:copy-dependencies package</code>

### Configure AwAlerting

<code> vi java/resources/aw-alerting-sample.properties</code>

 - Fill in the developer token, client ID / secret, manager account ID and
   refresh token.

<code> vi java/resources/aw-alerting-alerts-sample.json</code>

 - This is the JSON configuration of the alerts that you want to run. It's
   referred from the .properties file in the same folder.

## Run the project

<pre>
java -Xmx4G -jar aw-alerting.jar -file &lt;file&gt;

Arguments:

 -accountIdsFile &lt;file&gt;  Consider ONLY the account IDs specified on the file
                         to run the report

 -debug                  Display all the debug information. If option 'verbose'
                         is present, all the information will be displayed on
                         the console as well

 -file &lt;file&gt;            The properties file (please refer to the file
                         ./aw-alerting-sample.properties as an example)

 -help                   Display full help information
</pre>

## Implement custom report downloader

Alert report downloader is responsible for downloading report data for further
processing (e.g. apply rules and actions). We provide an implementation
[AwqlReportDownloader](https://github.com/googleads/aw-alerting/blob/master/java/com/google/api/ads/adwords/awalerting/sampleimpl/downloader/AwqlReportDownloader.java)
that downloads report data using AWQL. Custom alert report downloader:
 - Should derive from
   <code>com.google.api.ads.adwords.awalerting.downloader.AlertReportDownloader</code>,
   and
 - Must have a constructor with an AdWordsSession and a JsonObject parameters
   to work properly.

## Implement custom alert rules

Alert rules are responsible for:

 - Defining a list of field names to extend in the report
 - Determining a list of field values to extend for each report entry
 - Determining whether each report entry should be skipped from result alerts

Custom alert rules:
 - Should derive from
   <code>com.google.api.ads.adwords.awalerting.rule.AlertRule</code>, and
 - Must have a constructor with a JsonObject parameter to work properly, and
 - Must be stateless, since the same instance will be shared among multiple
   threads.

## Implement custom alert actions

Alert actions are responsible for processing each report entry, and:

 - Performing some action immediately, or
 - Recording some info and performing aggregated action at the end

Custom alert actions:
 - Should derive from
   <code>com.google.api.ads.adwords.awalerting.action.AlertAction</code>, and
 - Must have a constructor with a JsonObject parameter to work properly, and
 - Must NOT modify report entries (since an report entry may be processed by
   multiple alert actions). Any modification should be done by AlertRules.

## Plug custom report downloader, alert rules and alert actions

Just edit the JSON configuration file:

 - Under <code>"ReportDownloader"</code>, put class name of the custom alert
   report downloader in <code>"ClassName"</code> field, along with any other
   parameters that will be passed to the custom alert report downloader's
   constructor. For example:
 <pre>
    "ReportDownloader": {
      "ClassName": "AwqlReportDownloader",
      "ReportQuery": {
        "ReportType": "KEYWORDS_PERFORMANCE_REPORT",
        "Fields": "ExternalCustomerId,Id,Criteria,Impressions,Ctr",
        "Conditions": "Impressions > 100 AND Ctr < 0.05",
        "DateRange": "YESTERDAY"
      }
    }
 </pre>

 - Under <code>"Rules"</code>, put class name of the custom alert rule in
   <code>"ClassName"</code> field, along with other parameters that will be
   passed to the custom alert rule's constructor. For example:
 <pre>
    "Rules": [
      {
        "ClassName": "AddAccountManager"
      },
      {
        "ClassName": "AddAccountMonthlyBudget"
      }
    ]
 </pre>

 - Under <code>"Actions"</code>, put class name of the custom alert rule in
   <code>"ClassName"</code> field, along with other parameters that will be
   passed to the custom alert rule's constructor. For example:
 <pre>
    "Actions": [
      {
        "ClassName": "SimpleConsoleWriter"
      },
      {
        "ClassName": "PerAccountManagerEmailSender",
        "Subject": "Low impression accounts",
        "CC": "abc@example.com,xyz@example.com"
      }
    ]
 </pre>

### Fine print
Pull requests are very much appreciated. Please sign the
[Google Code contributor license agreement]
(http://code.google.com/legal/individual-cla-v1.0.html)
(There is a convenient online form) before submitting.

<dl>
  <dt>Copyright</dt>
  <dd>Copyright © 2015 Google, Inc.</dd>
  <dt>License</dt>
  <dd>Apache 2.0</dd>
  <dt>Limitations</dt>
  <dd>This is example software, use with caution under your own risk.</dd>
</dl>
