/**
 * (C) Copyright IBM Corp. 2015, 2022.
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
import static org.testng.Assert.assertNotNull;

import java.time.DateTimeException;
import java.util.Date;

import org.testng.annotations.Test;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.ibm.cloud.sdk.core.service.model.GenericModel;

/**
 * Tests related to the serialization and deserialization of date and date-time values.
 */
public class DateTimeSerializationTest {

  private boolean verbose = false;

  private void log(String msg) {
    if (verbose) {
      System.out.println(msg);
    }
  }

  // These functions use the same Gson configuration (registered type adapters, etc.)
  // as the java core uses during request process.
  private <T> T deserialize(String json, Class<T> clazz) {
    return GsonSingleton.getGsonWithoutPrettyPrinting().fromJson(json, clazz);
  }

  private String serialize(Object obj) {
    return GsonSingleton.getGsonWithoutPrettyPrinting().toJson(obj);
  }

  // This model simulates a generated model class with java.util.Date interpreted as a date-time.
  public class DateTimeModel extends GenericModel {
    // The estimated date-time at which the RedSox clinched one of their World Series wins.
    @SerializedName("ws_victory")
    public Date wsVictory;
  }

  // This model simulates a generated model class with java.util.Date interpreted as a full-date.
  public class DateModel extends GenericModel {
    // The estimated date-time at which the RedSox clinched one of their World Series wins.
    @JsonAdapter(DateTypeAdapter.class)
    @SerializedName("ws_victory")
    public Date wsVictory;
  }

  // Performs a round trip test using the model class T.
  private <T> void roundTripTest(String inputJson, String expectedOutputJson, Class<T> modelClass) {
    log("\nInitial JSON:    " + inputJson);

    T model = deserialize(inputJson, modelClass);
    assertNotNull(model);

    String outputJson = serialize(model);
    assertNotNull(outputJson);

    log("Serialized JSON: " + outputJson);

    assertEquals(outputJson, expectedOutputJson);
  }

  // Perform the round trip test using the model that interprets java.util.Date as a date-time.
  private void roundTripTestDateTime(String inputJson, String expectedOutputJson) {
    roundTripTest(inputJson, expectedOutputJson, DateTimeModel.class);
  }

  // Perform the round trip test using the model that interprets java.util.Date as a full-date.
  private void roundTripTestDate(String inputJson, String expectedOutputJson) {
    roundTripTest(inputJson, expectedOutputJson, DateModel.class);
  }

  @Test
  public void testModelsDateTime() {
    // RFC 3339 date-time with milliseconds with Z tz-offset.
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13T21:30:00.000Z\"}", "{\"ws_victory\":\"1903-10-13T21:30:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13T21:30:00.00011Z\"}", "{\"ws_victory\":\"1903-10-13T21:30:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13T21:30:00.0001134Z\"}", "{\"ws_victory\":\"1903-10-13T21:30:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13T21:30:00.000113456Z\"}", "{\"ws_victory\":\"1903-10-13T21:30:00.000Z\"}");

    // RFC 3339 date-time without milliseconds with Z tz-offset.
    roundTripTestDateTime("{\"ws_victory\":\"1912-10-16T19:34:00Z\"}", "{\"ws_victory\":\"1912-10-16T19:34:00.000Z\"}");

    // RFC 3339 date-time with milliseconds with non-Z tz-offset.
    roundTripTestDateTime("{\"ws_victory\":\"1915-10-13T16:15:00.000-03:00\"}", "{\"ws_victory\":\"1915-10-13T19:15:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1915-10-13T22:15:00.000+0300\"}",  "{\"ws_victory\":\"1915-10-13T19:15:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1915-10-13T16:15:00.000-03\"}",    "{\"ws_victory\":\"1915-10-13T19:15:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1915-10-13T22:15:00.000+03\"}",    "{\"ws_victory\":\"1915-10-13T19:15:00.000Z\"}");

    // RFC 3339 date-time without milliseconds with non-Z tz-offset.
    roundTripTestDateTime("{\"ws_victory\":\"1916-10-12T13:43:00-05:00\"}", "{\"ws_victory\":\"1916-10-12T18:43:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1916-10-12T13:43:00-05\"}",    "{\"ws_victory\":\"1916-10-12T18:43:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1916-10-12T21:13:00+0230\"}",  "{\"ws_victory\":\"1916-10-12T18:43:00.000Z\"}");

    // RFC 3339 with nanoseconds for the Catalog-Managements of the world.
    roundTripTestDateTime("{\"ws_victory\":\"1916-10-12T13:43:00.866305005-05:00\"}", "{\"ws_victory\":\"1916-10-12T18:43:00.866Z\"}");

    // UTC date-time with no tz.
    roundTripTestDateTime("{\"ws_victory\":\"1918-09-11T19:06:00.000\"}", "{\"ws_victory\":\"1918-09-11T19:06:00.000Z\"}");
    roundTripTestDateTime("{\"ws_victory\":\"1918-09-11T19:06:00\"}",     "{\"ws_victory\":\"1918-09-11T19:06:00.000Z\"}");

    // Dialog date-time.
    roundTripTestDateTime("{\"ws_victory\":\"2004-10-28 04:39:00\"}", "{\"ws_victory\":\"2004-10-28T04:39:00.000Z\"}");

    // Alchemy date-time.
    roundTripTestDateTime("{\"ws_victory\":\"20071029T043500\"}", "{\"ws_victory\":\"2007-10-29T04:35:00.000Z\"}");

    // RFC 3339 full-date.
    roundTripTestDateTime("{\"ws_victory\":\"2013-10-31\"}", "{\"ws_victory\":\"2013-10-31T00:00:00.000Z\"}");

    // Raw time value.
    roundTripTestDateTime("{\"ws_victory\":\"1540786620000\"}", "{\"ws_victory\":\"2018-10-29T04:17:00.000Z\"}");

    // Input is null
    roundTripTestDateTime("{\"ws_victory\": null}", "{}");
    roundTripTestDateTime("{}", "{}");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError1() {
    roundTripTestDateTime("{\"ws_victory\":\"x1903-10-13T21:30:00.000Z\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError2() {
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13T21:30:00.000X\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError3() {
    roundTripTestDateTime("{\"ws_victory\":\"1903-10-13X21:30:00.000Z\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError4() {
    roundTripTestDateTime("{\"ws_victory\":\"19031013T213000.000\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError5() {
    roundTripTestDateTime("{\"ws_victory\":\"03-10-13T21:30:00.000Z\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError6() {
    roundTripTestDateTime("{\"ws_victory\":\"03-10-13\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateTimeError7() {
    // too many digits in frac-sec part.
    roundTripTestDateTime("{\"ws_victory\":\"1916-10-12T13:43:00.8663050050-05:00\"}", "n/a");
  }

  @Test
  public void testModelsDate() {
    roundTripTestDate("{\"ws_victory\":\"1903-10-13\"}", "{\"ws_victory\":\"1903-10-13\"}");
    roundTripTestDate("{\"ws_victory\":\"1912-10-16\"}", "{\"ws_victory\":\"1912-10-16\"}");
    roundTripTestDate("{\"ws_victory\":\"1915-10-13\"}", "{\"ws_victory\":\"1915-10-13\"}");
    roundTripTestDate("{\"ws_victory\":\"1916-10-12\"}", "{\"ws_victory\":\"1916-10-12\"}");
    roundTripTestDate("{\"ws_victory\":\"1918-09-11\"}", "{\"ws_victory\":\"1918-09-11\"}");
    roundTripTestDate("{\"ws_victory\":\"2004-10-28\"}", "{\"ws_victory\":\"2004-10-28\"}");
    roundTripTestDate("{\"ws_victory\":\"2007-10-29\"}", "{\"ws_victory\":\"2007-10-29\"}");
    roundTripTestDate("{\"ws_victory\":\"2013-10-31\"}", "{\"ws_victory\":\"2013-10-31\"}");
    roundTripTestDate("{\"ws_victory\":\"2018-10-29\"}", "{\"ws_victory\":\"2018-10-29\"}");

    // Input is null
    roundTripTestDate("{\"ws_victory\": null}", "{}");
    roundTripTestDate("{}", "{}");
  }

  @Test
  public void testNullValues() {
    roundTripTestDateTime("{\"ws_victory\":null}", "{}");
    roundTripTestDate("{\"ws_victory\":null}", "{}");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateError1() {
    roundTripTestDate("{\"ws_victory\":\"18-10-29\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateError2() {
    roundTripTestDate("{\"ws_victory\":\"018-10-29\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateError3() {
    roundTripTestDate("{\"ws_victory\":\"2018-10-29x\"}", "n/a");
  }

  @Test(expectedExceptions = DateTimeException.class)
  public void testModelsDateError4() {
    roundTripTestDate("{\"ws_victory\":\"x2018-10-29\"}", "n/a");
  }

// This method was used to determine the raw time value.
//  @Test
//  public void testRawTime() {
//    String s = "2018-10-29T04:17:00.000Z";
//    Date d = DateUtils.parseAsDateTime(s);
//    log(String.format("%s raw = %d", s, d.getTime()));
//  }
}
