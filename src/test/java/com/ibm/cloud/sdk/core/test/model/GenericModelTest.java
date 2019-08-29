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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.test.model.generated.Foo;

/**
 * A few simple tests that exercise the GenericModel methods.
 */
public class GenericModelTest {

  private Foo createFoo(String foo, int bar) {
    Foo fooModel = new Foo();
    fooModel.setFoo(foo);
    fooModel.setBar(bar);
    return fooModel;
  }

  @Test
  public void testEquals() {
    Foo foo1 = createFoo("X", 33);
    Foo foo2 = createFoo("Y", 44);
    assertFalse(foo1.equals(foo2));

    assertTrue(foo1.equals(foo1));

    assertFalse(foo1.equals(null));

    assertFalse(foo1.equals(this));
  }

  @Test
  public void testHashCode() {
    Foo foo = createFoo("A", 38);
    assertNotEquals(foo.hashCode(), 0);
  }
}
