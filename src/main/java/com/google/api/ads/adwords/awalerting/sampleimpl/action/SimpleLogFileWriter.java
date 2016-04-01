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
import com.google.api.ads.adwords.awalerting.util.ConfigTags;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An alert action implementation that writes alert messages in the specified log file.
 * 
 * <p>The JSON config should look like:
 * <pre>
 * {
 *   "ActionClass": "SimpleLogFileWriter",
 *   "LogFilePathname": "/tmp/xyz.log",
 *   "AppendMode": "true"
 * }
 * </pre>
 */
public class SimpleLogFileWriter extends AlertAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLogFileWriter.class);

  private static final String LOG_FILE_PATHNAME_TAG = "LogFilePathname";
  
  // This config in JSON is optional, with default value as "true".
  private static final String APPEND_MODE_TAG = "AppendMode";

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

  private String filePathname;
  private BufferedWriter writer;

  public SimpleLogFileWriter(JsonObject config) throws IOException {
    super(config);
    
    filePathname = config.get(LOG_FILE_PATHNAME_TAG).getAsString();

    boolean appendMode = true;
    if (config.has(APPEND_MODE_TAG)) {
      appendMode = config.get(APPEND_MODE_TAG).getAsBoolean();
    }
    
    writer = new BufferedWriter(new FileWriter(filePathname, appendMode));
  }

  /**
   * Initialization action: print some header lines.
   */
  @Override
  public void initializeAction() throws AlertProcessingException {
    LOGGER.info("Start generating alerts into log file: {}", filePathname);

    try {
      Date now = new Date();
      writer.write("===== Begin of this run =====");
      writer.newLine();
      writer.write("Alerts generated at " + DATE_FORMAT.format(now) + ":");
      writer.newLine();
    } catch (IOException e) {
      throw new AlertProcessingException(
          "Error invoking SimpleLogFileWriter.initializeAction().", e);
    }
  }

  /**
   * Process a report entry, and write its alert message in the log file.
   *
   * @param entry the report entry to process
   */
  @Override
  public void processReportEntry(UnmodifiableReportRow entry) throws AlertProcessingException {
    try {
      writer.write(entry.getFieldValue(ConfigTags.ALERT_MESSAGE));
      writer.newLine();
    } catch (IOException e) {
      throw new AlertProcessingException(
          "Error invoking SimpleLogFileWriter.processReportEntry().", e);
    }
  }

  /**
   * Finalization action: print some foot lines.
   */
  @Override
  public void finalizeAction() throws AlertProcessingException {
    try {
      writer.write("===== End of this run =====");
      writer.newLine();
      writer.newLine();
      writer.close();
    } catch (IOException e) {
      throw new AlertProcessingException("Error invoking SimpleLogFileWriter.finalizeAction().", e);
    }

    LOGGER.info("Finish generating alerts into log file: {}", filePathname);
  }
}
