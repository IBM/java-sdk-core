/**
 * (C) Copyright IBM Corp. 2020.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.ibm.cloud.sdk.core.util;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * This class contains utilities for formatting and parsing {@link Date} instances as OpenAPI
 * "date" or "date-time" values.
 * <br><br>
 * References:
 * <dl>
 * <dt>API Handbook:</dt>
 * <dd>
 * <ul>
 * <li>https://cloud.ibm.com/docs/api-handbook?topic=api-handbook-types#date</li>
 * <li>https://cloud.ibm.com/docs/api-handbook?topic=api-handbook-types#date-time</li>
 * </ul>
 * </dd>
 * <dt>OpenAPI Specification:</dt>
 * <dd>
 * <ul>
 * <li>https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#data-types</li>
 * </ul>
 * </dd>
 * <dt>RFC 3339:</dt>
 * <dd>
 * <ul>
 * <li>https://tools.ietf.org/html/rfc3339#page-6</li>
 * </ul>
 * </dd>
 * </dl>
 */
public class DateUtils {

  //
  // These formatters are used to format (serialize) date and date-time values.
  // The use of ".withZone(UTC)" ensures that the output string will be
  // the UTC representation of the date or date-time value.
  //

  // This implements the RFC3339 "full-date" format.
  public static final DateTimeFormatter rfc3339FullDateFmt =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  // This implements the RFC3339 "date-time" format.
  public static final DateTimeFormatter rfc3339DateTimeFmt =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

  //
  // The following formatters are used for parsing date-time values.
  //

  // This is a partially-constructed formatter that will be used to construct other formatters below.
  // It implements: "yyyy-MM-dd'T'HH:mm"
  private static final DateTimeFormatter partialTimeBase = new DateTimeFormatterBuilder()
      .appendValue(YEAR_OF_ERA, 4, 19, SignStyle.EXCEEDS_PAD)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(DAY_OF_MONTH, 2)
      .appendLiteral('T')
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(":")
      .appendValue(MINUTE_OF_HOUR, 2)
      .toFormatter();

  // This is a partially-constructed formatter that will be used to construct other formatters below.
  // It implements the RFC 3339 "full-date" + "partial-time" format.
  // "yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]" (fractional part can be 0-9 digits).
  private static final DateTimeFormatter rfc3339BaseParser = new DateTimeFormatterBuilder()
      .append(partialTimeBase)
      .appendLiteral(":")
      .appendValue(SECOND_OF_MINUTE, 2)
      .optionalStart()
      .appendFraction(NANO_OF_SECOND, 0, 9, true)
      .toFormatter();

  // This implements the RFC3339 "date-time" format with either Z or +/-HH:MM (tz with colon delimiter).
  private static final DateTimeFormatter rfc3339DateTimeParser = new DateTimeFormatterBuilder()
      .append(rfc3339BaseParser)
      .appendOffset("+HH:MM", "Z")
      .toFormatter();

  // This implements the RFC3339 "date-time" format except with no colon in tz-offset (e.g. +/-HHMM).
  private static final DateTimeFormatter rfc3339DateTimeNoColonParser = new DateTimeFormatterBuilder()
      .append(rfc3339BaseParser)
      .appendOffset("+HHMM", "Z")
      .toFormatter();

  // This implements the RFC3339 "date-time" format except with only a 2-digit tz-offset (e.g. +/-HH).
  private static final DateTimeFormatter rfc3339DateTime2DigitTZParser = new DateTimeFormatterBuilder()
      .append(rfc3339BaseParser)
      .appendOffset("+HH", "Z")
      .toFormatter();

  // This implements a date-time value with no timezone component.  Assume UTC.
  private static final DateTimeFormatter utcDateTimeWithoutTZ =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]").withZone(ZoneOffset.UTC);

  // This implements the "Dialog" flavor of date-time with no timezone component. Assume UTC.
  private static final DateTimeFormatter dialogDateTimeParser =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  // This implements the "Alchemy" flavor of date-time with no timezone component. Assume UTC.
  private static final DateTimeFormatter alchemyDateTimeParser =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);

  // This implements the format used by IAM Identity: "yyyy-MM-dd'T'HH:mm+0000"
  private static final DateTimeFormatter iamIdentityParser = new DateTimeFormatterBuilder()
      .append(partialTimeBase)
      .appendOffset("+HHMM", "Z")
      .toFormatter();


  // This is the ordered list of parsers that we will use when trying to parse a particular date-time string.
  private static final List<DateTimeFormatter> dateTimeParsers =
      Arrays.asList(
          rfc3339DateTimeParser,         // "yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]X",  optional frac-sec, tz: 'Z' or -06:00
          rfc3339DateTimeNoColonParser,  // "yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]XXX" optional frac-sec, tz: 'Z' or -0600
          rfc3339DateTime2DigitTZParser, // "yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]XXX" optional frac-sec, tz: 'Z' or -06
          utcDateTimeWithoutTZ,          // "yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]"    optional frac-sec, no tz
          dialogDateTimeParser,          // "yyyy-MM-dd HH:mm:ss"                  no tz
          alchemyDateTimeParser,         // "yyyyMMdd'T'HHmmss"                    no tz
          iamIdentityParser              // "yyyy-MM-dd'T'HH:mmXXX"                no seconds, tz: Z or -0600)
          );

  // This regex is used to recognize a datetime expressed as # of milliseconds since epoch time.
  private static Pattern isJustNumber = Pattern.compile("^\\d+$");


  // Hide the ctor since this is a utility class.
  private DateUtils() {
  }

  /**
   * Formats the specified {@link Date} instance as an RFC 3339 "full-date" value (yyyy-MM-dd).<br>
   * The {@link Date} instance is assumed to represent the start of the specified day in UTC time.
   * No adjustment for timezone is performed while formatting the value.
   *
   * @param d the {@link Date} instance to be formatted
   * @return a string containing the RFC3339 "full-date" representation of "d" (example: 2020-01-01).
   * @throws DateTimeException if an error occurs during formatting
   */
  public static String formatAsDate(Date d) {
    return rfc3339FullDateFmt.format(d.toInstant());
  }

  /**
   * Formats the specified {@link Date} instance as an RFC 3339 "date-time" value
   * (a string of the form "yyyy-MM-dd'T'HH:mm:ss.SSSZ").<br>
   * The {@link Date} instance represents a moment in time (the number of milliseconds
   * since epoch time in UTC).<br>
   *
   * @param d the {@link Date} instance to be formatted
   * @return a string containing the UTC representation of "d" (example: 2020-01-01T12:00:00.000Z).
   * @throws DateTimeException if an error occurs during formatting
   */
  public static String formatAsDateTime(Date d) {
    return rfc3339DateTimeFmt.format(d.toInstant());
  }

  /**
   * Parses the specified string (assumed to be of the form "yyyy-MM-dd") into a {@link Date} instance.
   * Specifically, the string is parsed into a {@link Date} instance that represents
   * the start of the specified day in UTC time.
   * This is aligned with the {@link #formatAsDate} method which formats the {@link Date} instance
   * using "yyyy-MM-dd" without any adjustment for timezone.
   *
   * @param s the string to be parsed
   * @return the resulting {@link Date} value
   * @throws DateTimeException if an error occurs during parsing
   */
  public static Date parseAsDate(String s) {
    if (s == null) {
      return null;
    }

    s = s.trim();
    if (StringUtils.isEmpty(s)) {
      return null;
    }

    LocalDate ld = LocalDate.parse(s.trim());
    Instant instant = Instant.from(ld.atStartOfDay(ZoneId.of("UTC")));
    Date d = Date.from(instant);
    return d;
  }

  /**
   * Parses the specified string into a {@link Date} instance.
   * The supported formats are:
   * <ol>
   * <li>RFC 3339 "date-time": yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn]X  (optional ms, tz is 'Z' or +/-hh:mm)<br>
   *    Examples: 2020-01-01T12:00:00.000Z, 2020-01-01T07:00:00-05:00
   * </li>
   * <li>Same as above, but with no colon in tz-offset (e.g. -0300)<br>
   *    Examples: 2020-01-01T09:00:00.000-0300, 2020-01-01T16:00:00+0400
   * </li>
   * <li>Same as above, but with a 2-digit tz-offset (e.g. -03)<br>
   *    Examples: 2020-01-01T09:00:00.000-03, 2020-01-01T16:00:00+04
   * </li>
   * <li>UTC "date-time" with no tz: yyyy-MM-dd'T'HH:mm:ss[.nnnnnnnnn] (optional fractional seconds)<br>
   *    Examples: 2020-01-01T12:00:00.000, 2020-01-01T12:00:00
   * </li>
   * <li>"Dialog" date-time: yyyy-MM-dd HH:mm:ss<br>
   *    Examples: 2020-01-01 12:00:00
   * </li>
   * <li> "Alchemy" date-time: yyyyMMdd'T'HHmmss<br>
   *    Examples: 20200101T120000
   * </li>
   * <li> A "full-date": yyyy-MM-dd<br>
   *    Examples: 2020-01-01
   * </li>
   * <li> A raw time value (# of milliseconds since epoch time in UTC) <br>
   *    Examples: 1584024732866
   * </li>
   * </ol>
   *
   * @param dateAsString the string to be parsed
   * @return the resulting {@link Date} instance
   * @throws DateTimeException if an error occurs during parsing
   */
  public static Date parseAsDateTime(String dateAsString) {
    if (dateAsString == null) {
      return null;
    }

    dateAsString = dateAsString.trim();
    if (StringUtils.isEmpty(dateAsString)) {
      return null;
    }

    // First, try to parse using one of the supported date-time formatters.
    for (DateTimeFormatter formatter : dateTimeParsers) {
      try {
        return parse(dateAsString, formatter);
      } catch (Throwable e) {
        // absorb the exception
      }
    }

    // Next, try to parse the string as a RFC 3339 full date (yyyy-MM-dd).
    try {
      return parseAsDate(dateAsString);
    } catch (Throwable e) {
      // absorb the exception
    }

    // Finally, handle the scenario where the datetime value is simply expressed as
    // a long int (# of ms  since epoch time in UTC).
    Matcher foundMatch = isJustNumber.matcher(dateAsString);
    if (foundMatch.find()) {
      Long timeAsLong = Long.parseLong(dateAsString);
      return new Date(timeAsLong);
    }

    // If we failed to parse the string using the various formatters, throw an exception.
    throw new DateTimeException(String.format("Text '%s' could not be parsed as a date-time value.", dateAsString));
  }

  /**
   * Helper function to call the formatter's parse() method and convert result to a {@link Date} instance.
   *
   * @param s the string to parse
   * @param formatter the formatter to use
   * @return a {@link Date} instance
   */
  private static Date parse(String s, DateTimeFormatter formatter) {
    TemporalAccessor ta = formatter.parse(s);
    return Date.from(Instant.from(ta));
  }
}
