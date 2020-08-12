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

package com.ibm.cloud.sdk.core.test.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.test.model.generated.AnimalCat;

/**
 * A few simple tests that exercise the GenericModel methods.
 */
public class DynamicModelTest {
  private boolean displayOutput = false;

  private void log(String msg) {
    if (displayOutput) {
      System.out.println(msg);
    }
  }

  private AnimalCat createCat(String color, String propName, String propValue) {
    AnimalCat.Builder builder = new AnimalCat.Builder()
        .color(color)
        .animalType("Cat");
    if (propName != null) {
      builder.add(propName, propValue);
    }
    return builder.build();
  }

  @Test
  public void testEquals1() {
    AnimalCat cat1 = createCat("brown", null, null);
    assertNotNull(cat1);

    AnimalCat cat2 = createCat("brown", null, null);
    assertNotNull(cat2);

    assertEquals(cat1, cat2);
  }

  @Test
  public void testEquals2() {
    AnimalCat cat1 = createCat("brown", "prop", "value");
    assertNotNull(cat1);

    AnimalCat cat2 = createCat("brown", "prop", "value");
    assertNotNull(cat2);

    assertEquals(cat1, cat2);
    log("cat1: " + cat1.toString());
    log("cat2: " + cat2.toString());
  }

  @Test
  public void testNotEquals1() {
    AnimalCat cat1 = createCat("brown", "prop1", "value");
    assertNotNull(cat1);

    AnimalCat cat2 = createCat("brown", null, null);
    assertNotNull(cat2);

    assertNotEquals(cat1, cat2);
  }

  @Test
  public void testNotEquals2() {
    AnimalCat cat1 = createCat("brown", "prop1", "value");
    assertNotNull(cat1);

    AnimalCat cat2 = createCat("brown", "prop2", "value");
    assertNotNull(cat2);

    assertNotEquals(cat1, cat2);
  }

  @Test
  public void testNullAdditionalProperty() {
    AnimalCat cat = createCat("brown", "prop1", null);
    assertNotNull(cat);
    log("cat: " + cat.toString());

    assertTrue(cat.toString().contains("\"prop1\": null"));
  }

}
