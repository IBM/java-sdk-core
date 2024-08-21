/**
 * (C) Copyright IBM Corp. 2015, 2024.
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
import static org.testng.Assert.assertNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;

/**
 * Test the {@link GsonSingleton} class used to transform from and to JSON.
 */
public class GsonSingletonTest {

  public static class FooModel {
    public LazilyParsedNumber foo;
    public LazilyParsedNumber bar;
  }

  /** The list type. */
  private Type listType = new TypeToken<ArrayList<Date>>() { }.getType();
  private Type modelType = new TypeToken<FooModel>() { }.getType();

  /**
   * Test the date serializer and deserializer.
   */
  @Test
  public void testDateSerializer() {
    String dateAsJson = "[\"2014-06-04T15:38:07Z\"," + "\"2015-08-24T18:42:25.324Z\"," + "\"2015-08-24T18:42:25.324Z\","
        + "\"2015-08-31T00:49:27.77Z\"," + "\"2015-09-01T16:05:30.058-0400\"," + "\"2015-09-01T16:05:30.058-0400\","
        + "\"2015-09-01T16:05:30.058-0400\"," + "\"2015-09-01T16:05:30.058-0400\"," + "\"2015-10-08T17:59:39.609Z\","
        + "\"2016-03-12 20:31:58\"]";

    List<Date> dates = GsonSingleton.getGsonWithoutPrettyPrinting().fromJson(dateAsJson, listType);
    Assert.assertNotNull(dates);

    String datesAsString = GsonSingleton.getGsonWithoutPrettyPrinting().toJson(dates);
    Assert.assertNotNull(datesAsString);

    Assert.assertNotEquals(GsonSingleton.getGson().toJson(dates),
        GsonSingleton.getGsonWithoutPrettyPrinting().toJson(dates));
  }

  @Test
  public void testLazilyParsedNumber1() {
    String jsonString = "{\"foo\":38,\"bar\":28}";

    Gson gson = GsonSingleton.getGsonWithoutPrettyPrinting();

    FooModel model = gson.fromJson(jsonString, modelType);
    assertNotNull(model);
    assertEquals(model.foo.longValue(), new LazilyParsedNumber("38").longValue());
    assertEquals(model.bar.longValue(), new LazilyParsedNumber("28").longValue());

    String s = gson.toJson(model);
    assertEquals(s, jsonString);
  }

  @Test
  public void testLazilyParsedNumber2() {
    String jsonString = "{\"foo\":null,\"bar\":null}";

    Gson gson = GsonSingleton.getGsonWithSerializeNulls();

    FooModel model = gson.fromJson(jsonString, modelType);
    assertNotNull(model);
    assertNull(model.foo);
    assertNull(model.bar);

    String s = gson.toJson(model);
    assertEquals(s, jsonString);
  }

  @Test(expectedExceptions = { JsonSyntaxException.class })
  public void testLazilyParsedNumber3() {
    String jsonString = "{\"foo\":[\"numberlist\"]}";
    Gson gson = GsonSingleton.getGsonWithSerializeNulls();
    gson.fromJson(jsonString, modelType);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLazilyParsedNumber4() {
    Gson gson = GsonSingleton.getGsonWithoutPrettyPrinting();

    String jsonString = "{\"foo\":38.9999,\"bar\":28.0001,\"baz\":74}";
    Map<String, Object> map = gson.fromJson(jsonString, Map.class);
    assertNotNull(map);

    String serializedMap = gson.toJson(map);
    assertNotNull(serializedMap);
    assertEquals(serializedMap, jsonString);
  }
}
