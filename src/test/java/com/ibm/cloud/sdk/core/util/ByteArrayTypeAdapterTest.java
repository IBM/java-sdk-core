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

import static org.testng.Assert.assertEquals;

import org.junit.Test;

import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

/**
 *
 */
public class ByteArrayTypeAdapterTest {
  private boolean displayOutput = false;

  private String serialize(Object obj) {
    return GsonSingleton.getGson().toJson(obj);
  }

  private <T> T deserialize(String json, Class<T> clazz) {
    return GsonSingleton.getGson().fromJson(json, clazz);
  }

  private <T> void testSerDeser(T model, Class<T> clazz) {
    String jsonString = serialize(model);
    if (displayOutput) {
      System.out.println("serialized " + model.getClass().getSimpleName() + ": " + jsonString);
    }
    T newModel = deserialize(jsonString, clazz);
    if (displayOutput) {
      System.out.println("de-serialized " + model.getClass().getSimpleName() + ": " + newModel.toString());
    }
    assertEquals(newModel, model);
  }

  public class Model1 extends GenericModel {
    public String name;
    public byte[] bytes;

    public Model1(String name, byte[] bytes) {
      this.name = name;
      this.bytes = bytes;
    }
  }

  @Test
  public void testByteArray1() {
    Model1 model1 = new Model1("name1", new byte[] {01, 02, 03, 04, 05});

    testSerDeser(model1, Model1.class);
  }

  @Test
  public void testByteArray2() {
    String bytes = "This is a test of the emergency broadast system. This is only a test.";
    Model1 model1 = new Model1("name2", bytes.getBytes());

    testSerDeser(model1, Model1.class);
  }
}
