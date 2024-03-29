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

package com.ibm.cloud.sdk.core.test.http;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.NameValue;
import org.testng.annotations.BeforeMethod;

/**
 * The Class NameValueTest.
 */
public class NameValueTest {

  private NameValue fooBar;
  private NameValue foo;
  private NameValue fooBuzz;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @BeforeMethod
  public void setUp() throws Exception {
    fooBar = new NameValue("foo", "bar");
    foo = new NameValue("foo", null);
    fooBuzz = new NameValue("foo", "buzz");
  }

  /**
   * Test name null.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNameNull() {
    new NameValue(null, null);
  }

  /**
   * Test name equals.
   */
  @Test
  public void testEquals() {
    assertNameValueEquals(fooBar, new NameValue("foo", "bar"));
    assertNameValueEquals(foo, new NameValue("foo", null));
  }

  /**
   * Test inequality.
   */
  @Test
  public void testNotEquals() {
    assertNotEquals(fooBar, fooBuzz);
  }

  /**
   * Test value null.
   */
  @Test
  public void testNull() {
    assertEquals(foo.getName(), "foo");
    assertNull(foo.getValue());
    assertNotEquals(foo.hashCode(), fooBar.hashCode());
    assertNotEquals(foo, fooBar);
  }

  /**
   * Test toString().
   */
  @Test
  public void testToString() {
    assertEquals("foo=bar", fooBar.toString());
    assertEquals("foo", foo.toString());
  }

  /**
   * Assert equals.
   */
  private static void assertNameValueEquals(NameValue a, NameValue b) {
    assertEquals(a, b);
    assertEquals(a.toString(), b.toString());
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(b));
  }

}
