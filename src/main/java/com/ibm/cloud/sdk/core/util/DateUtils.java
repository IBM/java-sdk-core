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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  // RFC 3339 "full-date" used in formatting and parsing.
  private static final String RFC3339_FULL_DATE                = "yyyy-MM-dd";

  // RFC 3339 "date-time" used in formatting.
  private static final String RFC3339_DATE_TIME_FMT            = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  // RFC 3339 "date-time" used in parsing.
  // This flavor accepts either "Z" or tz offset with colon (+03:00)
  private static final String RFC3339_DATE_TIME_PARSE          = "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX";

  // This flavor accepts either "Z" or tz offset without colon (+0300)
  // Technically this isn't part of RFC 3339, but it's close enough.
  private static final String RFC3339_DATE_TIME_PARSE_NOCOLON  = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X";

  // UTC date-time with no tz (date-time is assumed to be expressed in UTC time).
  private static final String UTC_DATE_TIME_NO_TZ              = "yyyy-MM-dd'T'HH:mm:ss[.SSS]";

  // Date format used by Alchemy (UTC is assumed).
  private static final String ALCHEMY_DATE_TIME                = "yyyyMMdd'T'HHmmss";

  // Date format used by ??? (UTC is assumed).
  private static final String DIALOG_DATE_TIME                 = "yyyy-MM-dd HH:mm:ss";

  // These formatters are used to format (serialize) date and date-time values.
  // The use of ".withZone(UTC)" ensures that the output string will be
  // the UTC representation of the date or date-time value.
  private static final DateTimeFormatter rfc3339FullDateFormat =
      DateTimeFormatter.ofPattern(RFC3339_FULL_DATE).withZone(ZoneOffset.UTC);      // "yyyy-MM-dd"
  private static final DateTimeFormatter rfc3339DateTimeFormatter =
      DateTimeFormatter.ofPattern(RFC3339_DATE_TIME_FMT).withZone(ZoneOffset.UTC);  // "yyyy-MM-ddTHH:mm:ss.SSSZ"


  //
  // These formatters are used for parsing date-time values.
  //
  private static final DateTimeFormatter rfc3339DateTimeParse =
      DateTimeFormatter.ofPattern(RFC3339_DATE_TIME_PARSE);
  private static final DateTimeFormatter rfc3339DateTimeParseNoColon =
      DateTimeFormatter.ofPattern(RFC3339_DATE_TIME_PARSE_NOCOLON);
  private static final DateTimeFormatter utcDateTimeWithoutTZ =
      DateTimeFormatter.ofPattern(UTC_DATE_TIME_NO_TZ).withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter dialogDateTime =
      DateTimeFormatter.ofPattern(DIALOG_DATE_TIME).withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter alchemyDateTime =
      DateTimeFormatter.ofPattern(ALCHEMY_DATE_TIME).withZone(ZoneOffset.UTC);

  private static final List<DateTimeFormatter> dateTimeParsers =
      Arrays.asList(
          rfc3339DateTimeParse,            // "yyyy-MM-dd'T'HH:mm:ss[.SSS]X",  optional ms, tz: 'Z' or -03:00
          rfc3339DateTimeParseNoColon,     // "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX" optional ms, tz: 'Z' or -0300
          utcDateTimeWithoutTZ,            // "yyyy-MM-dd'T'HH:mm:ss[.SSS]"    optional ms
          dialogDateTime,                  // "yyyy-MM-dd HH:mm:ss"
          alchemyDateTime                  // "yyyyMMdd'T'HHmmss"
          );

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
    return rfc3339FullDateFormat.format(d.toInstant());
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
    return rfc3339DateTimeFormatter.format(d.toInstant());
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
    LocalDate ld = LocalDate.parse(s.trim());
    Instant instant = Instant.from(ld.atStartOfDay(ZoneId.of("UTC")));
    Date d = Date.from(instant);
    return d;
  }

  /**
   * Parses the specified string into a {@link Date} instance.
   * The supported formats are:
   * <ol>
   * <li>RFC 3339 "date-time": yyyy-MM-dd'T'HH:mm:ss[.SSS]X  (optional ms, tz is 'Z' or +/-hh:mm)<br>
   *    Examples: 2020-01-01T12:00:00.000Z, 2020-01-01T07:00:00-05:00
   * </li>
   * <li>Same as above, but with no colon in tz-offset (e.g. -0300)<br>
   *    Examples: 2020-01-01T09:00:00.000-0300, 2020-01-01T16:00:00+0400
   * </li>
   * <li>UTC "date-time" with no tz: yyyy-MM-dd'T'HH:mm:ss[.SSS] (optional ms)<br>
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
   *    Examples: 2020-01-01
   * </li>
   * </ol>
   *
   * @param dateAsString the string to be parsed
   * @return the resulting {@link Date} instance
   * @throws DateTimeException if an error occurs during parsing
   */
  public static Date parseAsDateTime(String dateAsString) {
    // First, try to parse using one of the supported date-time formatters.
    for (DateTimeFormatter format : dateTimeParsers) {
      try {
        return parse(dateAsString, format);
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
