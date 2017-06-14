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
import com.google.api.ads.adwords.awalerting.report.UnmodifiableReportRow;
import com.google.api.ads.adwords.awalerting.util.JdbcUtil;
import com.google.api.client.util.Lists;
import com.google.gson.JsonObject;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

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
@NotThreadSafe
public class SqlDbPersister implements AlertAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDbPersister.class);

  // config keys for database connection.
  private static final String DB_DRIVER_TAG = "Driver"; // optional
  private static final String DB_URL_TAG = "Url";
  private static final String DB_LOGIN_TAG = "Login"; // optional
  private static final String DB_PASSWORD_TAG = "Password"; // optional

  // default values.
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
  
  private int batchedInsertions;
  private int insertionsCount;
  
  private final JdbcTemplate jdbcTemplate;
  private final List<Object[]> batchArgs;
  
  public SqlDbPersister(JsonObject config) {
    jdbcTemplate =
        JdbcUtil.createJdbcTemplate(
            config, DB_DRIVER_TAG, DB_URL_TAG, DB_LOGIN_TAG, DB_PASSWORD_TAG);
    
    batchArgs = Lists.newArrayList();
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
      DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
      ResultSet result = metaData.getTables(null, DB_SCHEMA_NAME, DB_TABLE_NAME, null);
      if (!result.next()) {
        LOGGER.info("Creating the table {}.{} in database.", DB_SCHEMA_NAME, DB_TABLE_NAME);
        jdbcTemplate.execute(CREATE_TABLE_SQL);
      }
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
  public void processReportEntry(UnmodifiableReportRow entry) {
    Timestamp timestamp = new Timestamp(new Date().getTime());
    
    String clientCustomerIdStr = entry.getFieldValue("ExternalCustomerId");
    Long clientCustomerId = null;
    if (clientCustomerIdStr != null) {
      clientCustomerId = Long.valueOf(clientCustomerIdStr.replaceAll("-", ""));
    }
    
    String accountName = entry.getFieldValue("AccountDescriptiveName");
    String accountManagerName = entry.getFieldValue("AccountManagerName");
    String accountManagerEmail = entry.getFieldValue("AccountManagerEmail");
    String alertMessage = entry.getFieldValue("AlertMessage");
    
    batchArgs.add(
        new Object[] {
          timestamp,
          clientCustomerId,
          accountName,
          accountManagerName,
          accountManagerEmail,
          alertMessage
        });
    insertionsCount++;
    if (batchedInsertions++ >= BATCH_INSERTION_SIZE) {
      commitBatch();
    }
  }
  
  /**
   * Commit the batch of insertions.
   */
  private void commitBatch() {
    jdbcTemplate.batchUpdate(INSERT_ALERT_SQL, batchArgs);
    batchedInsertions = 0;
    batchArgs.clear();
  }

  /**
   * Finalization action: execute reminder batch insertions.
   */
  @Override
  public void finalizeAction() {
    if (batchedInsertions > 0) {
      commitBatch();
    }

    LOGGER.info("Inserted {} alert records into the database.", insertionsCount);
  }
}
