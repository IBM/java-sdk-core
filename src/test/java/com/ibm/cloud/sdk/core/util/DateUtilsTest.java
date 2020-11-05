/**
 * (C) Copyright IBM Corp. 2015, 2019.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.ibm.cloud.sdk.core.service.model.GenericModel;

/**
 * Tests related to the DateUtils class.
 */
@SuppressWarnings("deprecation")
public class DateUtilsTest {

  private String serialize(Object obj) {
    return GsonSingleton.getGson().toJson(obj);
  }

  private <T> T deserialize(String json, Class<T> clazz) {
    return GsonSingleton.getGson().fromJson(json, clazz);
  }

  @Test
  public void testDate() throws Exception {
    Date expectedDate = new Date(120, 10, 03);
    Date d = DateUtils.parseAsDate("2020-11-03");
    assertNotNull(d);
    assertEquals(expectedDate, d);

    String s = DateUtils.formatAsDate(d);
    assertNotNull(s);
    assertEquals("2020-11-03", s);
  }

  @Test
  public void testDateTime() throws Exception {
    Date expectedDate = new Date(120, 10, 03, 19, 01, 33);
    Date d = DateUtils.parseAsDateTime("2020-11-03T19:01:33.000");
    assertNotNull(d);
    assertEquals(expectedDate, d);

    String s = DateUtils.formatAsDateTime(d);
    assertNotNull(s);
    assertEquals("2020-11-03T19:01:33.000", s);
  }

  // Simulates a generated model with "date" and "date-time" fields.
  // This will exercise the OpenAPIDateSerializer and OpenAPIDateTimeSerializer classes.
  public class MyModel extends GenericModel {
    @JsonAdapter(OpenAPIDateSerializer.class)
    @SerializedName("date_field")
    public Date dateField;

    @JsonAdapter(OpenAPIDateTimeSerializer.class)
    @SerializedName("date_time_field")
    public Date dateTimeField;
  }

  @Test
  public void testModel() {
    MyModel myModel = new MyModel();
    myModel.dateField = new Date(120, 10, 03);
    myModel.dateTimeField = new Date(120, 10, 03, 19, 01, 33);

    String s = serialize(myModel);

    MyModel newModel = deserialize(s, MyModel.class);
    assertNotNull(newModel);
    assertEquals(myModel, newModel);
  }

  @Test(expected = JsonParseException.class)
  public void testModelErrorDate() {
    String badJson = "{\"date_field\": \"bad-date\", \"date_time_field\": \"2020-11-03T19:01:33.000\"}";
    deserialize(badJson, MyModel.class);
  }

  @Test(expected = JsonParseException.class)
  public void testModelErrorDateTime() {
    String badJson = "{\"date_field\": \"2020-11-03\", \"date_time_field\": \"bad-date-time\"}";
    deserialize(badJson, MyModel.class);
  }
}
