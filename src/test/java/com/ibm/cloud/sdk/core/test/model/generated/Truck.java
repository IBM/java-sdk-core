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

/**
 * Information about a truck.
 */
public class Truck extends Vehicle {

  @SerializedName("engine_type")
  protected String engineType;

  /**
   * Builder.
   */
  public static class Builder {
    private String vehicleType;
    private String make;
    private String engineType;

    private Builder(Truck truck) {
      this.vehicleType = truck.vehicleType;
      this.make = truck.make;
      this.engineType = truck.engineType;
    }

    /**
     * Instantiates a new builder.
     */
    public Builder() {
    }

    /**
     * Instantiates a new builder with required properties.
     *
     * @param vehicleType the vehicleType
     */
    public Builder(String vehicleType) {
      this.vehicleType = vehicleType;
    }

    /**
     * Builds a Truck.
     *
     * @return the truck
     */
    public Truck build() {
      return new Truck(this);
    }

    /**
     * Set the vehicleType.
     *
     * @param vehicleType the vehicleType
     * @return the Truck builder
     */
    public Builder vehicleType(String vehicleType) {
      this.vehicleType = vehicleType;
      return this;
    }

    /**
     * Set the make.
     *
     * @param make the make
     * @return the Truck builder
     */
    public Builder make(String make) {
      this.make = make;
      return this;
    }

    /**
     * Set the engineType.
     *
     * @param engineType the engineType
     * @return the Truck builder
     */
    public Builder engineType(String engineType) {
      this.engineType = engineType;
      return this;
    }
  }

  protected Truck(Builder builder) {
    // Removed this validation to allow for negative tests.
    // com.ibm.cloud.sdk.core.util.Validator.notNull(builder.vehicleType, "vehicleType cannot be null");
    vehicleType = builder.vehicleType;
    make = builder.make;
    engineType = builder.engineType;
  }

  /**
   * New builder.
   *
   * @return a Truck builder
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Gets the engineType.
   *
   * @return the engineType
   */
  public String engineType() {
    return engineType;
  }
}

