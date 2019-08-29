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

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.service.model.DynamicModel;

/**
 * Model with additionalProperties set to an integer schema.
 */
public class ModelAPInteger extends DynamicModel<Long> {
  @SerializedName("prop1")
  private String prop1;
  @SerializedName("prop2")
  private Long prop2;

  public ModelAPInteger() {
    super(new TypeToken<Long>(){});
  }

  /**
   * Gets the prop1.
   *
   * String property.
   *
   * @return the prop1
   */
  public String prop1() {
    return this.prop1;
  }

  /**
   * Sets the prop1.
   *
   * @param prop1 the new prop1
   */
  public void setProp1(final String prop1) {
    this.prop1 = prop1;
  }

  /**
   * Gets the prop2.
   *
   * Integer property.
   *
   * @return the prop2
   */
  public Long prop2() {
    return this.prop2;
  }

  /**
   * Sets the prop2.
   *
   * @param prop2 the new prop2
   */
  public void setProp2(final long prop2) {
    this.prop2 = prop2;
  }
}
