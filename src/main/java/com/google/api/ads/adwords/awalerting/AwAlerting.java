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

package com.google.api.ads.adwords.awalerting;

import com.google.api.ads.adwords.awalerting.processor.AlertProcessor;
import com.google.api.ads.adwords.awalerting.util.DynamicPropertyPlaceholderConfigurer;
import com.google.api.ads.adwords.awalerting.util.JaxWsProxySelector;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Main class that executes the alerts processing logic.
 *
 * <p>This class holds a Spring application context that manages the creation of all the beans
 * needed.
 *
 * <p>Credentials and properties are pulled from the ~/aw-report-alerting-sample.properties file
 * or -file <file> provided.
 *
 * <p>See README for more info.
 */
public class AwAlerting {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwAlerting.class);

  /**
   * The Spring application context used to get all the beans.
   */
  private static ApplicationContext appCtx;

  /**
   * Main method.
   *
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    // Set up proxy.
    JaxWsProxySelector ps = new JaxWsProxySelector(ProxySelector.getDefault());
    ProxySelector.setDefault(ps);

    Options options = createCommandLineOptions();
    try {
      CommandLineParser parser = new BasicParser();
      CommandLine cmdLine = parser.parse(options, args);

      // Print full help and quit
      if (cmdLine.hasOption("help")) {
        printHelpMessage(options);
        printSamplePropertiesFile();
        System.exit(0);
      }

      if (!cmdLine.hasOption("file")) {
        LOGGER.error("Missing required option: 'file'");
        System.exit(1);
      }

      processAlerts(cmdLine);
    } catch (ParseException e) {
      LOGGER.error("Error parsing the values for the command line options.", e);
      System.exit(1);
    } catch (AlertConfigLoadException e) {
      LOGGER.error("Error laoding alerts configuration.", e);
      System.exit(1);
    } catch (AlertProcessingException e) {
      LOGGER.error("Error processing alerts.", e);
      System.exit(1);
    }
  }

  /**
   * Main logic for creating processor and processing alerts according to configuration.
   *
   * @param cmdLine the command line arguments
   */
  private static void processAlerts(CommandLine cmdLine)
      throws AlertConfigLoadException, AlertProcessingException {
    String propertiesPath = cmdLine.getOptionValue("file");
    JsonObject alertsConfig = getAlertsConfig(propertiesPath);

    LOGGER.debug("Creating ReportProcessor bean...");
    AlertProcessor processor = createAlertProcessor();
    LOGGER.debug("... success.");

    LOGGER.info("*** Retrieving account IDs ***");
    Set<Long> accountIdsSet = null;
    if (cmdLine.hasOption("accountIdsFile")) {
      String accountsFileName = cmdLine.getOptionValue("accountIdsFile");
      accountIdsSet = getAccountsFromFile(accountsFileName);
      LOGGER.info("Accounts loaded from file: {}.", accountsFileName);
    }
    // If no "accountIdsFile" option, it will pass "accountIdsSet" as null and later load all
    // accounts under the manager account.

    processor.generateAlerts(accountIdsSet, alertsConfig);
  }

  /**
   * Load JSON configuration from the properties file.
   *
   * @param propertiesPath the path to the properties file
   * @return JSON configuration loaded from the json file
   * @throws AlertConfigLoadException error reading properties / json file
   */
  private static JsonObject getAlertsConfig(String propertiesPath) throws AlertConfigLoadException {
    LOGGER.info("Using properties file: {}", propertiesPath);

    Resource propertiesResource = new ClassPathResource(propertiesPath);
    if (!propertiesResource.exists()) {
      propertiesResource = new FileSystemResource(propertiesPath);
    }
    
    JsonObject alertsConfig = null;
    try {
      Properties properties = initApplicationContextAndProperties(propertiesResource);

      // Load alerts config from the same folder as properties file
      String alertsConfigFilename = properties.getProperty("aw.alerting.alerts");
      String propertiesFolder = propertiesResource.getFile().getParent();
      File alertsConfigFile = new File(propertiesFolder, alertsConfigFilename);

      // If it does not exist, try the default resource folder
      if (!alertsConfigFile.exists()) {
        String alertsConfigFilepath = "java/resources/" + alertsConfigFilename;
        alertsConfigFile = new File(alertsConfigFilepath);
      }

      LOGGER.debug("Loading alerts config file from {}", alertsConfigFile.getAbsolutePath());
      JsonParser jsonParser = new JsonParser();
      alertsConfig = jsonParser.parse(new FileReader(alertsConfigFile)).getAsJsonObject();
      LOGGER.debug("Done.");
    } catch (IOException e) {
      throw new AlertConfigLoadException("Error loading alerts config at " + propertiesPath, e);
    } catch (JsonParseException e) {
      throw new AlertConfigLoadException("Error parsing config file at " + propertiesPath, e);
    }

    return alertsConfig;
  }


  /**
   * Read the account ids from the stream.
   *
   * @param stream the stream to be read
   * @return a set of account ids in the file
   */
  protected static Set<Long> getAccountsFromStream(InputStream stream)
      throws AlertConfigLoadException {
    Set<Long> accountIdsSet = new HashSet<Long>();
    LOGGER.debug("Account IDs to be queried:");
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      String line;
      while ((line = reader.readLine()) != null) {
        String accountIdAsString = line.replaceAll("-", "");
        Long accountId = Long.valueOf(accountIdAsString);
        accountIdsSet.add(accountId);
        LOGGER.debug("Account ID: {}", accountId);
      }
      reader.close();
    } catch (IOException e) {
      throw new AlertConfigLoadException("Error reading accounts from stream.", e);
    }

    return accountIdsSet;
  }
  
  /**
   * Reads the account ids from specified file path.
   *
   * @param filepath the file path to be read from
   * @return a set of account ids in the file
   */
  private static Set<Long> getAccountsFromFile(String filepath)
      throws AlertConfigLoadException {
    InputStream stream = null;
    try {
      stream = new FileInputStream(filepath);
    } catch (FileNotFoundException e) {
      throw new AlertConfigLoadException("Error reading accounts from file path: " + filepath, e);
    }
    
    return getAccountsFromStream(stream);
  }

  /**
   * Creates the {@link AlertProcessor} autowiring all the dependencies.
   *
   * @return the {@code AlertProcessor} with all the dependencies properly injected
   */
  private static AlertProcessor createAlertProcessor() {
    return appCtx.getBean(AlertProcessor.class);
  }

  /**
   * Creates the command line options.
   *
   * @return the {@link Options}
   */
  private static Options createCommandLineOptions() {
    Options options = new Options();
    Option help = new Option("help", "print this message");
    options.addOption(help);

    OptionBuilder.withArgName("file");
    OptionBuilder.hasArg(true);
    OptionBuilder.withDescription("aw-report-alerting-sample.properties file.");
    OptionBuilder.isRequired(false);
    options.addOption(OptionBuilder.create("file"));

    OptionBuilder.withArgName("accountIdsFile");
    OptionBuilder.hasArg(true);
    OptionBuilder.withDescription(
        "Consider ONLY the account IDs specified on the file to run the report");
    OptionBuilder.isRequired(false);
    options.addOption(OptionBuilder.create("accountIdsFile"));

    OptionBuilder.withArgName("debug");
    OptionBuilder.hasArg(false);
    OptionBuilder.withDescription("Will display all the debug information. "
        + "If the option 'verbose' is activated, "
        + "all the information will be displayed on the console as well");
    OptionBuilder.isRequired(false);
    options.addOption(OptionBuilder.create("debug"));

    return options;
  }

  /**
   * Prints the help message.
   *
   * @param options the options available for the user
   */
  private static void printHelpMessage(Options options) {
    // automatically generate the help statement
    HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(120);
    formatter.printHelp(
        "\n  java -Xmx1G -jar aw-alerting.jar -file <file>", "\nArguments:", options, "");
    System.out.println();
  }

  /**
   * Prints the sample properties file on the default output.
   */
  private static void printSamplePropertiesFile() throws AlertConfigLoadException {
    System.out.println("File: aw-report-alerting-sample.properties example");
    ClassPathResource sampleFile = new ClassPathResource("aw-alerting-sample.properties");

    try {
      List<String> lines =
          Files.asCharSource(sampleFile.getFile(), Charset.defaultCharset()).readLines();
      for (String line : lines) {
        System.out.println(line);
      }
    } catch (IOException e) {
      throw new AlertConfigLoadException("Error reading sample properties file.", e);
    }
  }

  /**
   * Initialize the application context, adding the properties configuration file depending on the
   * specified path.
   *
   * @param propertiesResource the properties resource
   * @return the resource loaded from the properties file
   * @throws IOException error opening the properties file
   */
  private static Properties initApplicationContextAndProperties(Resource propertiesResource)
      throws IOException {
    LOGGER.trace("Innitializing Spring application context.");
    DynamicPropertyPlaceholderConfigurer.setDynamicResource(propertiesResource);
    Properties properties = PropertiesLoaderUtils.loadProperties(propertiesResource);

    // Selecting the XMLs to choose the Spring Beans to load.
    List<String> listOfClassPathXml = new ArrayList<String>();
    listOfClassPathXml.add("classpath:aw-alerting-processor-beans.xml");

    appCtx = new ClassPathXmlApplicationContext(
        listOfClassPathXml.toArray(new String[listOfClassPathXml.size()]));

    return properties;
  }
}
