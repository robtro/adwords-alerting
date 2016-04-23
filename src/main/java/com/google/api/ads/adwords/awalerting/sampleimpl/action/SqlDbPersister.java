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

package com.google.api.ads.adwords.awalerting.sampleimpl.action;

import com.google.api.ads.adwords.awalerting.AlertAction;
import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.api.ads.adwords.awalerting.report.ReportRow;
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

/**
 * An alert action implementation that persists alert messages into database.
 * For simplicity, it just uses JDBC instead of Hibernate.
 * 
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ClassName": "SqlDbPersister",
 *   "Driver": "...",
 *   "Url": "...",
 *   "Login": "...",
 *   "Password": "..."
 * }
 * </pre>
 */
public class SqlDbPersister extends AlertAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDbPersister.class);

  // config keys for database connection.
  private static final String DB_DRIVER_TAG = "Driver"; // optional
  private static final String DB_URL_TAG = "Url";
  private static final String DB_LOGIN_TAG = "Login"; // optional
  private static final String DB_PASSWORD_TAG = "Password"; // optional

  // default values.
  private static final String DEFAULT_DB_DRIVER = "com.mysql.jdbc.Driver";
  private static final int BATCH_INSERTION_SIZE = 100; // number of insertions in a batch

  private static final String DB_SCHEMA_NAME = "AWAlerting";
  private static final String DB_TABLE_NAME = "AW_Alerts";
  
  private static final String CREATE_TABLE_SQL = "CREATE TABLE " + DB_TABLE_NAME
      + " (TIMESTAMP datetime,"
      + " ACCOUNT_ID bigint,"
      + " ACCOUNT_DESCRIPTIVE_NAME varchar(255),"
      + " ACCOUNT_MANAGER_NAME varchar(255),"
      + " ACCOUNT_MANAGER_EMAIL varchar(255),"
      + " ALERT_MESSAGE text not null,"
      + " INDEX ACCOUNT_ID_INDEX (ACCOUNT_ID))";
  
  private static final String INSERT_ALERT_SQL = "INSERT INTO " + DB_TABLE_NAME
      + " (TIMESTAMP,"
      + " ACCOUNT_ID,"
      + " ACCOUNT_DESCRIPTIVE_NAME,"
      + " ACCOUNT_MANAGER_NAME,"
      + " ACCOUNT_MANAGER_EMAIL,"
      + " ALERT_MESSAGE)"
      + " VALUES (?, ?, ?, ?, ?, ?)";
  
  private Connection dbConnection;
  private PreparedStatement preparedStatement;
  private int batchedInsertions;
  private int insertionsCount;
  
  public SqlDbPersister(JsonObject config) throws ClassNotFoundException, SQLException {
    super(config);
    
    String driver = DEFAULT_DB_DRIVER;
    if (config.has(DB_DRIVER_TAG)) {
      driver = config.get(DB_DRIVER_TAG).getAsString();
    }

    Preconditions.checkArgument(
        config.has(DB_URL_TAG), "Missing compulsory property: %s", DB_URL_TAG);
    String url = config.get(DB_URL_TAG).getAsString();

    String login = null;
    if (config.has(DB_LOGIN_TAG)) {
      login = config.get(DB_LOGIN_TAG).getAsString();
    }

    String password = null;
    if (config.has(DB_PASSWORD_TAG)) {
      password = config.get(DB_PASSWORD_TAG).getAsString();
    }

    // Register driver.
    Class.forName(driver);

    // Open a DB connection.
    if (login == null && password == null) {
      dbConnection = DriverManager.getConnection(url);
    } else {
      dbConnection = DriverManager.getConnection(url, login, password);
    }
  }

  /**
   * Initialization action: prepare database table and statement.
   */
  @Override
  public void initializeAction() throws AlertProcessingException {
    batchedInsertions = 0;
    insertionsCount = 0;

    try {
      // Check if table already exists, if not create it.
      DatabaseMetaData metaData = dbConnection.getMetaData();
      ResultSet result = metaData.getTables(null, DB_SCHEMA_NAME, DB_TABLE_NAME, null);
      if (!result.next()) {
        Statement statement = dbConnection.createStatement();
        LOGGER.info(CREATE_TABLE_SQL);
        statement.executeUpdate(CREATE_TABLE_SQL);
        statement.close();
      }

      // Create prepared statement.
      preparedStatement = dbConnection.prepareStatement(INSERT_ALERT_SQL);

      // Enable batch insertion.
      dbConnection.setAutoCommit(false);
    } catch (SQLException e) {
      throw new AlertProcessingException("Error invoking SQLDBPersister.initializeAction().", e);
    }
  }
  
  /**
   * Process a report entry, and insert information into database.
   * 
   * @param entry the report entry to process
   */
  @Override
  public void processReportEntry(UnmodifiableReportRow entry) throws AlertProcessingException {
    try {
      prepareStatement(entry);
      insertStatement();
    } catch (SQLException e) {
      throw new AlertProcessingException("Error invoking SQLDBPersister.processReportEntry().", e);
    }
  }

  /**
   * Prepare the report entry for batch insertion.
   *
   * @param entry the report entry to process
   */
  private void prepareStatement(ReportRow entry) throws SQLException {
    preparedStatement.setTimestamp(1, new Timestamp(new Date().getTime()));

    String clientCustomerIdStr = entry.getFieldValue("ExternalCustomerId");
    if (clientCustomerIdStr != null) {
      long clientCustomerId = Long.parseLong(clientCustomerIdStr.replaceAll("-", ""));
      preparedStatement.setLong(2, clientCustomerId);
    }

    String accountName = entry.getFieldValue("AccountDescriptiveName");
    if (accountName != null) {
      preparedStatement.setString(3, accountName);
    }

    String accountManagerName = entry.getFieldValue("AccountManagerName");
    if (accountManagerName != null) {
      preparedStatement.setString(4, accountManagerName);
    }

    String accountManagerEmail = entry.getFieldValue("AccountManagerEmail");
    if (accountManagerEmail != null) {
      preparedStatement.setString(5, accountManagerEmail);
    }

    String alertMessage = entry.getFieldValue("AlertMessage");
    preparedStatement.setString(6, alertMessage);
  }

  /**
   * Add the statement into batch insertion, and commit if batch is ready.
   */
  private void insertStatement() throws SQLException {
    preparedStatement.addBatch();
    insertionsCount++;
    if (batchedInsertions++ >= BATCH_INSERTION_SIZE) {
      commitInsertions();
    }
  }

  /**
   * Finalization action: execute reminder batch insertions, then close statement and database
   * connection.
   */
  @Override
  public void finalizeAction() throws AlertProcessingException {
    try {
      if (batchedInsertions > 0) {
        commitInsertions();
      }

      // Close statement and database connection.
      preparedStatement.close();
      dbConnection.close();
    } catch (SQLException e) {
      throw new AlertProcessingException("Error invoking SQLDBPersister.finalizeAction().", e);
    }

    LOGGER.info("Inserted {} alert records into the database.", insertionsCount);
  }
  
  /**
   * Commit the insertion statements in batch.
   */
  private void commitInsertions() throws SQLException {
    preparedStatement.executeBatch();
    dbConnection.commit();
    batchedInsertions = 0;
  }
}
