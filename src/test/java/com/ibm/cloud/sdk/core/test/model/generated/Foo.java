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

package com.ibm.cloud.sdk.core.test.model.generated;

import com.ibm.cloud.sdk.core.service.model.GenericModel;

/**
 * A named schema used with additionalProperties.
 */
public class Foo extends GenericModel {

  private String foo;
  private Long bar;

  /**
   * Gets the foo.
   *
   * String property.
   *
   * @return the foo
   */
  public String getFoo() {
    return foo;
  }

  /**
   * Gets the bar.
   *
   * Integer property.
   *
   * @return the bar
   */
  public Long getBar() {
    return bar;
  }

  /**
   * Sets the foo.
   *
   * @param foo the new foo
   */
  public void setFoo(final String foo) {
    this.foo = foo;
  }

  /**
   * Sets the bar.
   *
   * @param bar the new bar
   */
  public void setBar(final long bar) {
    this.bar = bar;
  }
}

