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

import java.util.ArrayList;

import org.testng.annotations.Test;

/**
 * The Class ValidatorTest.
 */
public class ValidatorTest {

  /** The error. */
  private final String error = "error message";

  /**
   * Test is true boolean string.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testIsTrueBooleanString() {
    Validator.isTrue(false, error);
  }

  /**
   * Test not empty collection string.
   */
  @SuppressWarnings("rawtypes")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotEmptyCollectionString() {
    Validator.notEmpty(new ArrayList(), error);
  }

  /**
   * Test not empty object array string.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotEmptyObjectArrayString() {
    Validator.notEmpty(new String[] { }, error);
  }

  /**
   * Test not empty string string.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotEmptyStringString() {
    Validator.notEmpty("", error);
  }

  /**
   * Test not null object string.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotNullObjectString() {
    Validator.notNull(null, error);
  }
}
