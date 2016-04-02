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

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.AlertReportDownloader;
import com.google.api.ads.adwords.awalerting.report.ReportData;
import com.google.api.ads.adwords.awalerting.util.DateRange;
import com.google.api.ads.adwords.jaxws.v201603.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to download report data from database (such as aw-reporting's local database).
 * Note that it must have a constructor that takes an AdWordsSession and a JsonObject.
 *
 * <p>The JSON config should look like:
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
 *     "Table": "AW_ReportKeywords",
 *     "ColumnMappings": [
 *       {
 *         "DatabaseColumnName": "ACCOUNT_ID",
 *         "ReportDataHeaderName": "ExternalCustomerId"
 *       },
 *       {
 *         "DatabaseColumnName": "ACCOUNT_DESCRIPTIVE_NAME",
 *         "ReportDataHeaderName": "AccountDescriptiveName"
 *       },
 *       {
 *         "DatabaseColumnName": "KEYWORD_ID",
 *         "ReportDataHeaderName": "Id"
 *       },
 *       {
 *         "DatabaseColumnName": "CRITERIA",
 *         "ReportDataHeaderName": "Criteria"
 *       },
 *       {
 *         "DatabaseColumnName": "IMPRESSIONS",
 *         "ReportDataHeaderName": "Impressions"
 *       },
 *       {
 *         "DatabaseColumnName": "CTR",
 *         "ReportDataHeaderName": "Ctr"
 *       }
 *     ],
 *     "Conditions": "Impressions > 100 AND Ctr < 0.05",
 *     "DateRange": "YESTERDAY"
 *   }
 * }
 * </pre>
 */
public class SqlDbReportDownloader extends AlertReportDownloader {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDbReportDownloader.class);

  // config keys for database connection.
  private static final String DATABASE_TAG = "Database";
  private static final String DRIVER_TAG = "Driver";
  private static final String URL_TAG = "Url";
  private static final String LOGIN_TAG = "Login";
  private static final String PASSWORD_TAG = "Password";

  // config keys for database query.
  private static final String REPORT_QUERY_TAG = "ReportQuery";
  private static final String TABLE_TAG = "Table";
  private static final String COLUMN_MAPPINGS_TAG = "ColumnMappings";
  private static final String DATABASE_COLUMN_NAME_TAG = "DatabaseColumnName";
  private static final String REPORT_HEADER_NAME_TAG = "ReportDataHeaderName";
  private static final String CONDITIONS_TAG = "Conditions";
  private static final String DATE_RANGE_TAG = "DateRange";

  private static final String DEFAULT_DB_DRIVER = "com.mysql.jdbc.Driver";

  private static final String DATE_COLUMN_NAME = "Day";
  private static final String DATA_RANGE_CONDITION_FORMAT = "DATE(%s) BETWEEN %s AND %s";
  private static final String REPORT_HEADER_NAME_CID = "ExternalCustomerId";

  private JsonObject config;

  public SqlDbReportDownloader(AdWordsSession session, JsonObject config) {
    super(session, config);
    this.config = config;
  }

  @Override
  public List<ReportData> downloadReports(Set<Long> accountIds) throws AlertProcessingException {
    Map<String, ReportData> reportDataMap = new HashMap<String, ReportData>();

    Connection dbConnection = null;
    Statement statement = null;
    ResultSet result = null;

    try {
      dbConnection = getDbConnection();
      LOGGER.info(
          "Downloading report data from SQL server: {}", dbConnection.getMetaData().getURL());

      List<String> header = new ArrayList<String>();
      String sqlQuery = getSqlQueryWithHeader(header);
      LOGGER.info("Using the following query: {}", sqlQuery);

      statement = dbConnection.createStatement();
      result = statement.executeQuery(sqlQuery);

      int columns = header.size();
      // SQL query and ResultSet should match in # of columns
      Preconditions.checkState(columns == result.getMetaData().getColumnCount());

      Map<String, Integer> indexMapping = new HashMap<String, Integer>(columns);
      for (int i = 0; i < columns; i++) {
        indexMapping.put(header.get(i), Integer.valueOf(i));
      }
      
      Preconditions.checkArgument(
          indexMapping.containsKey(REPORT_HEADER_NAME_CID),
          "You must choose AccountId field to generate report data");
      int indexCid = indexMapping.get(REPORT_HEADER_NAME_CID);

      while (result.next()) {
        List<String> row = new ArrayList<String>(columns);
        for (int columnCount = 1; columnCount <= columns; columnCount++) {
          row.add(result.getString(columnCount));
        }
       
        String cid = row.get(indexCid);
        ReportData reportData = reportDataMap.get(cid);
        if (reportData == null) {
          // ReportDefinitionReportType doesn't really matter (just for some printing purpose),
          // so don't bother find it from database table name.
          reportData = new ReportData(ReportDefinitionReportType.UNKNOWN, header, indexMapping);
          reportDataMap.put(cid, reportData);
        }
        reportData.addRow(row);
      }
    } catch (SQLException e) {
      throw new AlertProcessingException("Failed to query database!", e);
    } finally {
      // Close all resources
      if (result != null) {
        try {
          result.close();
        } catch (SQLException e) {
          // do nothing
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          // do nothing
        }
      }

      if (dbConnection != null) {
        try {
          dbConnection.close();
        } catch (SQLException e) {
          // do nothing
        }
      }
    }

    return new ArrayList<ReportData>(reportDataMap.values());
  }

  /**
   * Establish database connection according to config.
   */
  private Connection getDbConnection() throws AlertProcessingException {
    Preconditions.checkArgument(
        config.has(DATABASE_TAG), "Missing compulsory property: %s", DATABASE_TAG);
    JsonObject dbConfig = config.get(DATABASE_TAG).getAsJsonObject();

    String driver = DEFAULT_DB_DRIVER;
    if (dbConfig.has(DRIVER_TAG)) {
      driver = dbConfig.get(DRIVER_TAG).getAsString();
    }

    Preconditions.checkArgument(
        dbConfig.has(URL_TAG), "Missing compulsory property: %s - %s", DATABASE_TAG, URL_TAG);
    String url = dbConfig.get(URL_TAG).getAsString();

    String login = null;
    if (dbConfig.has(LOGIN_TAG)) {
      login = dbConfig.get(LOGIN_TAG).getAsString();
    }

    String password = null;
    if (dbConfig.has(PASSWORD_TAG)) {
      password = dbConfig.get(PASSWORD_TAG).getAsString();
    }

    Connection dbConnection;
    try {
      // Register driver.
      Class.forName(driver);

      // Open a DB connection.
      if (login == null && password == null) {
        dbConnection = DriverManager.getConnection(url);
      } else {
        dbConnection = DriverManager.getConnection(url, login, password);
      }
    } catch (ClassNotFoundException e) {
      throw new AlertProcessingException("Cannot find driver class: " + driver, e);
    } catch (SQLException e) {
      throw new AlertProcessingException("Fail to establish database exception.", e);
    }

    return dbConnection;
  }

  /**
   * Get SQL query string according to config, and build header list.
   * @param header an empty list of string to be filled with report header
   * @return the sql query string
   */
  private String getSqlQueryWithHeader(List<String> header) {
    Preconditions.checkArgument(
        config.has(REPORT_QUERY_TAG), "Missing compulsory property: %s", REPORT_QUERY_TAG);
    JsonObject queryConfig = config.get(REPORT_QUERY_TAG).getAsJsonObject();

    StringBuilder sqlQueryBuilder = new StringBuilder();
    sqlQueryBuilder.append("SELECT ");

    Preconditions.checkArgument(
        queryConfig.has(COLUMN_MAPPINGS_TAG),
        "Missing compulsory property: %s - %s",
        REPORT_QUERY_TAG,
        COLUMN_MAPPINGS_TAG);
    JsonArray columnMappings = queryConfig.getAsJsonArray(COLUMN_MAPPINGS_TAG);
    // Use LinkedHashMap to preserve order (SQL query and result parsing must have matched order).
    Map<String, String> fields = new LinkedHashMap<String, String>(columnMappings.size());

    // Process database column -> report header mapping
    header.clear();
    String dbColumnName, reportHeaderName;
    for (JsonElement columnMapping : columnMappings) {
      JsonObject mapping = columnMapping.getAsJsonObject();
      Preconditions.checkArgument(
          mapping.has(DATABASE_COLUMN_NAME_TAG),
          "Missing compulsory property: %s - %s - %s",
          REPORT_QUERY_TAG,
          COLUMN_MAPPINGS_TAG,
          DATABASE_COLUMN_NAME_TAG);
      Preconditions.checkArgument(
          mapping.has(REPORT_HEADER_NAME_TAG),
          "Missing compulsory property: %s - %s - %s",
          REPORT_QUERY_TAG,
          COLUMN_MAPPINGS_TAG,
          REPORT_HEADER_NAME_TAG);
      dbColumnName = mapping.get(DATABASE_COLUMN_NAME_TAG).getAsString();
      reportHeaderName = mapping.get(REPORT_HEADER_NAME_TAG).getAsString();
      fields.put(dbColumnName, reportHeaderName);
      header.add(reportHeaderName);
    }

    sqlQueryBuilder.append(Joiner.on(", ").withKeyValueSeparator(" AS ").join(fields));

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

    return sqlQueryBuilder.toString();
  }
}
