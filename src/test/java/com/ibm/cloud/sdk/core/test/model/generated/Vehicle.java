/*
 * (C) Copyright IBM Corp. 2019.
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
import com.ibm.cloud.sdk.core.service.model.GenericModel;

/**
 * Generic information about a vehicle.
 */
public class Vehicle extends GenericModel {
  @SuppressWarnings("unused")
  protected static String discriminatorPropertyName = "vehicle_type";
  protected static java.util.Map<String, Class<?>> discriminatorMapping;
  static {
    discriminatorMapping = new java.util.HashMap<>();
    discriminatorMapping.put("truck", Truck.class);
  }

  @SerializedName("vehicle_type")
  protected String vehicleType;
  protected String make;

  /**
   * Gets the vehicleType.
   *
   * @return the vehicleType
   */
  public String vehicleType() {
    return vehicleType;
  }

  /**
   * Gets the make.
   *
   * @return the make
   */
  public String make() {
    return make;
  }
}

