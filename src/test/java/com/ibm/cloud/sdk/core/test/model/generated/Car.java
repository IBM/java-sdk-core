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
 * Information about a car.
 */
public class Car extends Vehicle {

  @SerializedName("body_style")
  protected String bodyStyle;

  /**
   * Builder.
   */
  public static class Builder {
    private String vehicleType;
    private String make;
    private String bodyStyle;

    private Builder(Car car) {
      this.vehicleType = car.vehicleType;
      this.make = car.make;
      this.bodyStyle = car.bodyStyle;
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
     * Builds a Car.
     *
     * @return the car
     */
    public Car build() {
      return new Car(this);
    }

    /**
     * Set the vehicleType.
     *
     * @param vehicleType the vehicleType
     * @return the Car builder
     */
    public Builder vehicleType(String vehicleType) {
      this.vehicleType = vehicleType;
      return this;
    }

    /**
     * Set the make.
     *
     * @param make the make
     * @return the Car builder
     */
    public Builder make(String make) {
      this.make = make;
      return this;
    }

    /**
     * Set the bodyStyle.
     *
     * @param bodyStyle the bodyStyle
     * @return the Car builder
     */
    public Builder bodyStyle(String bodyStyle) {
      this.bodyStyle = bodyStyle;
      return this;
    }
  }

  protected Car(Builder builder) {
    // Removed this validation to allow for negative tests.
    // com.ibm.cloud.sdk.core.util.Validator.notNull(builder.vehicleType, "vehicleType cannot be null");
    vehicleType = builder.vehicleType;
    make = builder.make;
    bodyStyle = builder.bodyStyle;
  }

  /**
   * New builder.
   *
   * @return a Car builder
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Gets the bodyStyle.
   *
   * @return the bodyStyle
   */
  public String bodyStyle() {
    return bodyStyle;
  }
}

