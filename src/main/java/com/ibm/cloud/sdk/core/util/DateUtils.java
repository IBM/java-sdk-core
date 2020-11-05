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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class contains utilities for formatting java.util.Date instances as a
 * "date" or a "date-time" value. In this context, "date" and "date-time" refer
 * to the data and datetime types that are part of the OpenAPI Specification.
 */
public class DateUtils {
  private static final SimpleDateFormat rfc3339FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat utcDateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

  // Hide the ctor since this is a utility class.
  private DateUtils() {
  }

  /**
   * Formats the specified Date value as an OpenAPI "date"
   * (a string of the form "yyyy-MM-dd").
   *
   * @param d the Date instance to be formatted
   * @return a string representing an OpenAPI "date" (yyyy-MM-dd).
   */
  public static String formatAsDate(Date d) {
    String s;
    synchronized (rfc3339FullDateFormatter) {
      s = rfc3339FullDateFormatter.format(d);
    }
    return s;
  }

  /**
   * Parses the specified string (assumed to be of the form yyyy-MM-dd) as a Date value.
   *
   * @param s the string to be parsed
   * @return the resulting Date value
   * @throws ParseException error during parsing
   */
  public static Date parseAsDate(String s) throws ParseException {
    Date d;
    synchronized (rfc3339FullDateFormatter) {
      d = rfc3339FullDateFormatter.parse(s);
    }
    return d;
  }

  /**
   * Formats the specified Date value as an OpenAPI "date-time"
   * (a string of the form "yyyy-MM-dd'T'HH:mm:ss.SSS").
   *
   * @param d the Date instance to be formatted
   * @return a string representing an OpenAPI "date-time" (yyyy-MM-dd'T'HH:mm:ss.SSS).
   */
  public static String formatAsDateTime(Date d) {
    String s;
    synchronized (utcDateTimeFormatter) {
      s = utcDateTimeFormatter.format(d);
    }
    return s;
  }

  /**
   * Parses the specified string (assumed to be of the form yyyy-MM-dd'T'HH:mm:ss.SSS) as a Date value.
   *
   * @param s the string to be parsed
   * @return the resulting Date value
   * @throws ParseException error during parsing
   */
  public static Date parseAsDateTime(String s) throws ParseException {
    Date d;
    synchronized (utcDateTimeFormatter) {
      d = utcDateTimeFormatter.parse(s);
    }
    return d;
  }
}
