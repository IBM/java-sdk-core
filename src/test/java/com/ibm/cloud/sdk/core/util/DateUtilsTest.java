/**
 * (C) Copyright IBM Corp. 2020.
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.time.DateTimeException;
import java.util.Date;

import org.testng.annotations.Test;

/**
 * Tests related to the DateUtils class.
 */
public class DateUtilsTest {

  private boolean verbose = false;

  private void log(String s) {
    if (verbose) {
      System.out.println(s);
    }
  }

  @Test
  public void testNullEmpty() {
    assertNull(DateUtils.parseAsDate(null));
    assertNull(DateUtils.parseAsDateTime(null));
    assertNull(DateUtils.parseAsDate(""));
    assertNull(DateUtils.parseAsDateTime(""));
  }

  @Test
  public void testFullDate() {
    _testFullDate("1970-01-01");
    _testFullDate("1963-01-01");
    _testFullDate("2020-11-01");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testFullDateError1() {
    _testFullDate("2020-01-01x");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testFullDateError2() {
    _testFullDate("x2020-01-01");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testFullDateError3() {
    _testFullDate("20200101");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testFullDateError4() {
    _testFullDate("not-a-date");
  }

  @Test
  public void testDateTime() {

    // RFC 3339 Full Date.
    _testDateTime("1970-01-01", "1970-01-01T00:00:00.000Z");

    // RFC 3339 with various flavors of tz-offset
    _testDateTime("2016-06-20T04:25:16.218Z",     "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16.218+0000", "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16.218+00",   "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16.218-0000", "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16.218-00",   "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T00:25:16.218-0400", "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T00:25:16.218-04",   "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T07:25:16.218+0300", "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T07:25:16.218+03",   "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16Z",         "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T04:25:16+0000",     "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T04:25:16-0000",     "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T01:25:16-0300",     "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T01:25:16-03:00",    "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T08:55:16+04:30",    "2016-06-20T04:25:16.000Z");
    _testDateTime("2016-06-20T16:25:16+12:00",    "2016-06-20T04:25:16.000Z");

    // RFC 3339 with nanoseconds for the Catalog-Managements of the world.
    _testDateTime("2020-03-12T10:52:12.866305005-04:00", "2020-03-12T14:52:12.866Z");
    _testDateTime("2020-03-12T10:52:12.866305005Z",      "2020-03-12T10:52:12.866Z");
    _testDateTime("2020-03-12T10:52:12.866305005+02:30", "2020-03-12T08:22:12.866Z");
    _testDateTime("2020-03-12T10:52:12.866305Z",         "2020-03-12T10:52:12.866Z");

    // UTC datetime with no TZ.
    _testDateTime("2016-06-20T04:25:16.218",      "2016-06-20T04:25:16.218Z");
    _testDateTime("2016-06-20T04:25:16",          "2016-06-20T04:25:16.000Z");

    // Dialog datetime.
    _testDateTime("2016-06-20 04:25:16",          "2016-06-20T04:25:16.000Z");

    // Alchemy datetime.
    _testDateTime("20160620T042516",              "2016-06-20T04:25:16.000Z");

    // IAM Identity Service.
    _testDateTime("2020-11-10T12:28+0000", "2020-11-10T12:28:00.000Z");
    _testDateTime("2020-11-10T07:28-0500", "2020-11-10T12:28:00.000Z");
    _testDateTime("2020-11-10T12:28Z",     "2020-11-10T12:28:00.000Z");

    // RFC 2616 HTTP Date.
    _testDateTime("Fri, 31 Dec 1999 23:59:59 GMT", "1999-12-31T23:59:59.000Z");
    _testDateTime("Fri, 31 Dec 1999 23:59:59 CT", "2000-01-01T05:59:59.000Z");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testDateTimeError1() {
    _testDateTime("2016-06-20T04:25:16.218+000", "");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testDateTimeError2() {
    _testDateTime("20160620 042516", "");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testDateTimeError3() {
    _testDateTime("20160620T12:00:00", "");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testDateTimeError4() {
    _testDateTime("x2016-06-20T04:25:16.218+000", "");
  }

  private void _testFullDate(String s) {
    Date d = DateUtils.parseAsDate(s);
    String dateString = DateUtils.formatAsDate(d);
    log(String.format("testDate: input=%s, dateString=%s, Date.time=%d", s, dateString, d.getTime()));
    assertEquals(s, dateString);
  }

  private void _testDateTime(String s, String expectedUTC) {
    Date d = DateUtils.parseAsDateTime(s);
    String dateString = DateUtils.formatAsDateTime(d);
    log(String.format("testDateTime: input=%s, dateString=%s, Date.time=%d", s, dateString, d.getTime()));
    assertEquals(expectedUTC, dateString);
  }
}
