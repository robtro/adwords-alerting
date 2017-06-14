// Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.api.ads.adwords.awalerting.sampleimpl.downloader;

import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.DateRange;
import com.google.api.ads.adwords.awalerting.util.JdbcUtil;
import com.google.api.ads.adwords.jaxws.v201705.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Class to download report data from database (such as aw-reporting's local database).
 *
 * <p>
 * The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "SqlDbReportDownloader",
 *   "Database": {
 *     "Driver": "com.mysql.jdbc.Driver",
 *     "Url": "jdbc:mysql://localhost:3306/AWReports",
 *     "Login": "reportuser",
 *     "Password": "1234"
 *   },
 *   "ReportQuery": {
 *     "ReportType": "ACCOUNT_PERFORMANCE_REPORT",
 *     "Table": "AW_ReportKeywords",
 *     "ColumnMappings": [
 *       {
 *         "DatabaseColumnName": "ACCOUNT_ID",
 *         "ReportDataColumnName": "ExternalCustomerId"
 *       },
 *       {
 *         "DatabaseColumnName": "ACCOUNT_DESCRIPTIVE_NAME",
 *         "ReportDataColumnName": "AccountDescriptiveName"
 *       },
 *       {
 *         "DatabaseColumnName": "KEYWORD_ID",
 *         "ReportDataColumnName": "Id"
 *       },
 *       {
 *         "DatabaseColumnName": "CRITERIA",
 *         "ReportDataColumnName": "Criteria"
 *       },
 *       {
 *         "DatabaseColumnName": "IMPRESSIONS",
 *         "ReportDataColumnName": "Impressions"
 *       },
 *       {
 *         "DatabaseColumnName": "CTR",
 *         "ReportDataColumnName": "Ctr"
 *       }
 *     ],
 *     "Conditions": "Impressions > 100 AND Ctr < 0.05",
 *     "DateRange": "YESTERDAY"
 *   }
 * }
 * </pre>
 */
public class SqlDbReportDownloader implements AlertReportDownloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDbReportDownloader.class);

  // Config keys for database connection.
  private static final String DATABASE_TAG = "Database";
  private static final String DRIVER_TAG = "Driver";
  private static final String URL_TAG = "Url";
  private static final String LOGIN_TAG = "Login";
  private static final String PASSWORD_TAG = "Password";

  // Config keys for database query.
  private static final String REPORT_QUERY_TAG = "ReportQuery";
  private static final String REPORT_TYPE_TAG = "ReportType";
  private static final String TABLE_TAG = "Table";
  private static final String COLUMN_MAPPINGS_TAG = "ColumnMappings";
  private static final String DATABASE_COLUMN_NAME_TAG = "DatabaseColumnName";
  private static final String REPORT_COLUMN_NAME_TAG = "ReportDataColumnName";
  private static final String CONDITIONS_TAG = "Conditions";
  private static final String DATE_RANGE_TAG = "DateRange";

  private static final String DATE_COLUMN_NAME = "Day";
  private static final String DATA_RANGE_CONDITION_FORMAT = "DATE(%s) BETWEEN %s AND %s";
  private static final String EXTERNAL_CUSTOMER_ID_REPORT_COLUMN_NAME = "ExternalCustomerId";

  private JsonObject config;

  public SqlDbReportDownloader(JsonObject config) {
    this.config = config;
  }

  @Override
  public List<ReportData> downloadReports(
      ImmutableAdWordsSession protoSession, Set<Long> clientCustomerIds) {
    Map<Long, ReportData> reportDataMap = new HashMap<Long, ReportData>();
    
    JdbcTemplate jdbcTemplate = getJdbcTemplate();
    String sqlQuery = getSqlQueryWithReportColumnNames();
    ReportDefinitionReportType reportType = getReportType();
    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlQuery);
    
    // Get the column index of customer id. 
    int customerIdColumnIndex = rowSet.findColumn(EXTERNAL_CUSTOMER_ID_REPORT_COLUMN_NAME);
    Preconditions.checkState(
        customerIdColumnIndex >= 0,
        "You must choose \"%s\" field to generate report data",
        EXTERNAL_CUSTOMER_ID_REPORT_COLUMN_NAME);
    
    List<String> columnNames = Arrays.asList(rowSet.getMetaData().getColumnNames());
    int columns = columnNames.size();
    
    // Read result into map.
    int rows = 0;
    while (rowSet.next()) {
      rows++;
      List<String> row = new ArrayList<String>(columns);
      for (int i = 0; i < columns; i++) {
        row.add(rowSet.getString(i));
      }

      String customerIdStr = row.get(customerIdColumnIndex);
      Long customerId = Long.parseLong(customerIdStr);
      ReportData reportData = reportDataMap.get(customerId);
      if (reportData == null) {
        reportData = new ReportData(customerId, reportType, columnNames);
        reportDataMap.put(customerId, reportData);
      }
      reportData.addRow(row);
    }
    
    LOGGER.info("Retrieved and parsed {} rows from database.", rows);
    return new ArrayList<ReportData>(reportDataMap.values());
  }

  /**
   * Create a JcbcTemplate according to config.
   */
  private JdbcTemplate getJdbcTemplate() {
    Preconditions.checkArgument(
        config.has(DATABASE_TAG), "Missing compulsory property: %s", DATABASE_TAG);
    JsonObject dbConfig = config.get(DATABASE_TAG).getAsJsonObject();
    
    return JdbcUtil.createJdbcTemplate(dbConfig, DRIVER_TAG, URL_TAG, LOGIN_TAG, PASSWORD_TAG);
  }
  
  private JsonObject getQueryConfig() {
    Preconditions.checkArgument(
        config.has(REPORT_QUERY_TAG), "Missing compulsory property: %s", REPORT_QUERY_TAG);
    return config.get(REPORT_QUERY_TAG).getAsJsonObject();
  }

  /**
   * Get SQL query string according to config, and build column names list.
   *
   * @return the sql query string
   */
  private String getSqlQueryWithReportColumnNames() {
    JsonObject queryConfig = getQueryConfig();
    
    StringBuilder sqlQueryBuilder = new StringBuilder();
    sqlQueryBuilder.append("SELECT ");

    Preconditions.checkArgument(
        queryConfig.has(COLUMN_MAPPINGS_TAG),
        "Missing compulsory property: %s - %s",
        REPORT_QUERY_TAG,
        COLUMN_MAPPINGS_TAG);
    JsonArray columnMappings = queryConfig.getAsJsonArray(COLUMN_MAPPINGS_TAG);
    // Use LinkedHashMap to preserve order (SQL query and result parsing must have matched order).
    Map<String, String> fieldsMapping = new LinkedHashMap<String, String>(columnMappings.size());

    // Process database column -> report column mapping
    String dbColumnName;
    String reportColumnName;
    for (JsonElement columnMapping : columnMappings) {
      JsonObject mapping = columnMapping.getAsJsonObject();
      Preconditions.checkArgument(
          mapping.has(DATABASE_COLUMN_NAME_TAG),
          "Missing compulsory property: %s - %s - %s",
          REPORT_QUERY_TAG,
          COLUMN_MAPPINGS_TAG,
          DATABASE_COLUMN_NAME_TAG);
      Preconditions.checkArgument(
          mapping.has(REPORT_COLUMN_NAME_TAG),
          "Missing compulsory property: %s - %s - %s",
          REPORT_QUERY_TAG,
          COLUMN_MAPPINGS_TAG,
          REPORT_COLUMN_NAME_TAG);
      dbColumnName = mapping.get(DATABASE_COLUMN_NAME_TAG).getAsString();
      reportColumnName = mapping.get(REPORT_COLUMN_NAME_TAG).getAsString();
      fieldsMapping.put(dbColumnName, reportColumnName);
    }

    sqlQueryBuilder.append(Joiner.on(", ").withKeyValueSeparator(" AS ").join(fieldsMapping));

    Preconditions.checkArgument(
        queryConfig.has(TABLE_TAG),
        "Missing compulsory property: %s - %s",
        REPORT_QUERY_TAG,
        TABLE_TAG);
    sqlQueryBuilder.append(" FROM ").append(queryConfig.get(TABLE_TAG).getAsString());

    boolean hasWhereClause = false;
    if (queryConfig.has(DATE_RANGE_TAG)) {
      DateRange dateRange = DateRange.fromString(queryConfig.get(DATE_RANGE_TAG).getAsString());
      String dateRangeCondition =
          String.format(
              DATA_RANGE_CONDITION_FORMAT,
              DATE_COLUMN_NAME,
              dateRange.getStartDate(),
              dateRange.getEndDate());
      sqlQueryBuilder.append(" WHERE ").append(dateRangeCondition);
      hasWhereClause = true;
    }

    if (queryConfig.has(CONDITIONS_TAG)) {
      sqlQueryBuilder
          .append(hasWhereClause ? " AND " : " WHERR ")
          .append(queryConfig.get(CONDITIONS_TAG).getAsString());
    }
    
    String sqlQuery = sqlQueryBuilder.toString();
    LOGGER.info("SQL query: {}", sqlQuery);
    return sqlQuery;
  }
  
  /**
   * Get report type from the config, defaulting to the unknown type.
   * @return the report type
   */
  private ReportDefinitionReportType getReportType() {
    JsonObject queryConfig = getQueryConfig();
    
    ReportDefinitionReportType reportType = ReportDefinitionReportType.UNKNOWN;
    if (queryConfig.has(REPORT_TYPE_TAG)) {
      String reportTypeStr = queryConfig.get(REPORT_TYPE_TAG).getAsString();
      reportType = ReportDefinitionReportType.valueOf(reportTypeStr);
    }

    return reportType;
  }
}
