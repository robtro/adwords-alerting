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

import com.google.api.ads.adwords.lib.jaxb.v201603.ReportDefinitionDateRangeType;
import com.google.api.client.util.Preconditions;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Class to represent start date and end date of user specified date range.
 */
public class DateRange {
  private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd").withZoneUTC();

  private final LocalDate startDate;
  private final LocalDate endDate;

  /**
   * Make constructor private so it can only be created by factory method.
   */
  private DateRange(LocalDate startDate, LocalDate endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String getStartDate() {
    return startDate.toString(formatter);
  }

  public String getEndDate() {
    return endDate.toString(formatter);
  }

  /**
   * Factory method to create a DataRange instance.
   * @param dateRange the date range string, either in "yyyyMMdd,yyyyMMdd" format, or some
   *                  ReportDefinitionDateRangeType enum value (such as "LAST_7_DAYS")
   * @return the DateRange object
   */
  public static DateRange fromString(String dateRange) {
    Preconditions.checkNotNull(dateRange, "DateRange cannot be null!");
    Preconditions.checkArgument(!dateRange.isEmpty(), "DateRange cannot be empty!");

    return dateRange.contains(",") ? parseCustomFormat(dateRange) : parseEnumFormat(dateRange);
  }

  /**
   * Parse DateRange in "yyyyMMdd,yyyyMMdd" format.
   */
  private static DateRange parseCustomFormat(String dateRange) {
    String[] dates = dateRange.split(",");
    Preconditions.checkArgument(dates.length == 2, "Unknown DateRange format: %s.", dateRange);

    // Just throws exception if argument is not in proper format "yyyyMMdd"
    LocalDate startDate = formatter.parseLocalDate(dates[0].trim());
    LocalDate endDate = formatter.parseLocalDate(dates[1].trim());
    return new DateRange(startDate, endDate);
  }

  /**
   * Parse DateRange in ReportDefinitionDateRangeType enum format.
   */
  private static DateRange parseEnumFormat(String dateRange) {
    ReportDefinitionDateRangeType dateRangeType;
    try {
      dateRangeType = ReportDefinitionDateRangeType.valueOf(dateRange);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown DateRange type: " + dateRange);
    }

    LocalDate today = LocalDate.now();
    LocalDate startDate;
    LocalDate endDate;
    switch (dateRangeType) {
      case TODAY:
        startDate = endDate = today;
        break;
      case YESTERDAY:
        startDate = endDate = today.minusDays(1);
        break;
      case LAST_7_DAYS:
        startDate = today.minusDays(7);
        endDate = today.minusDays(1);
        break;
      case LAST_WEEK:
        LocalDate.Property lastWeekProp = today.minusWeeks(1).dayOfWeek();
        startDate = lastWeekProp.withMinimumValue();
        endDate = lastWeekProp.withMaximumValue();
        break;
      case THIS_MONTH:
        LocalDate.Property thisMonthProp = today.dayOfMonth();
        startDate = thisMonthProp.withMinimumValue();
        endDate = thisMonthProp.withMaximumValue();
        break;
      case LAST_MONTH:
        LocalDate.Property lastMonthProp = today.minusMonths(1).dayOfMonth();
        startDate = lastMonthProp.withMinimumValue();
        endDate = lastMonthProp.withMaximumValue();
        break;
      case LAST_14_DAYS:
        startDate = today.minusDays(14);
        endDate = today.minusDays(1);
        break;
      case LAST_30_DAYS:
        startDate = today.minusDays(30);
        endDate = today.minusDays(1);
        break;
      case THIS_WEEK_SUN_TODAY:
        // Joda-Time uses the ISO standard Monday to Sunday week.
        startDate = today.minusWeeks(1).dayOfWeek().withMaximumValue();
        endDate = today;
        break;
      case THIS_WEEK_MON_TODAY:
        startDate = today.dayOfWeek().withMinimumValue();
        endDate = today;
        break;
      case LAST_WEEK_SUN_SAT:
        startDate = today.minusWeeks(2).dayOfWeek().withMaximumValue();
        endDate = today.minusWeeks(1).dayOfWeek().withMaximumValue().minusDays(1);
        break;
        // Don't support the following enums
      case LAST_BUSINESS_WEEK:
      case ALL_TIME:
      case CUSTOM_DATE:
      default:
        throw new IllegalArgumentException("Unsupported DateRange type: " + dateRange);
    }

    return new DateRange(startDate, endDate);
  }
}
