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

package com.google.api.ads.adwords.awalerting.util;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Utility class for Jdbc connectivity.
 */
public class JdbcUtil {
  // Default to MySQL JDBC driver.
  private static final String DEFAULT_DB_DRIVER = "com.mysql.jdbc.Driver";

  /**
   * Create a JdbcTemplate according to the parameters.
   *
   * @param dbConfig the Json configuration for database connection
   * @param driverKey the key for JDBC driver class name
   * @param urlKey the key for JDBC url
   * @param usernameKey the key for JDBC username
   * @param passwordKey the key JDBC password
   * @return a JdbcTemplate with the given parameters
   */
  public static JdbcTemplate createJdbcTemplate(JsonObject dbConfig,
      String driverKey, String urlKey, String usernameKey, String passwordKey) {
    // Read the config values.
    String driver = DEFAULT_DB_DRIVER;
    if (dbConfig.has(driverKey)) {
      driver = dbConfig.get(driverKey).getAsString();
    }
    
    Preconditions.checkArgument(dbConfig.has(urlKey), "Missing compulsory property: %s", urlKey);
    String url = dbConfig.get(urlKey).getAsString();
    
    String username = null;
    if (dbConfig.has(usernameKey)) {
      username = dbConfig.get(usernameKey).getAsString();
    }

    String password = null;
    if (dbConfig.has(passwordKey)) {
      password = dbConfig.get(passwordKey).getAsString();
    }
    
    // Create the data source.
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    
    // Create the JdbcTemplate with the data srouce.
    return new JdbcTemplate(dataSource);
  }  
}