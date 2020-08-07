/*
 * (C) Copyright IBM Corp. 2019, 2020.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Information about a cat.
 */
public class AnimalCat extends Animal {


  public AnimalCat() {
    super();
  }

  /**
   * Builder.
   */
  public static class Builder {
    private String animalType;
    private String color;
    private Map<String, String> dynamicProperties;

    public Builder(Animal animalCat) {
      this.animalType = animalCat.animalType;
      this.color = animalCat.color;
      this.dynamicProperties = animalCat.getProperties();
    }

    /**
     * Instantiates a new builder.
     */
    public Builder() {
    }

    /**
     * Instantiates a new builder with required properties.
     *
     * @param animalType the animalType
     * @param color the color
     */
    public Builder(String animalType, String color) {
      this.animalType = animalType;
      this.color = color;
    }

    /**
     * Builds a AnimalCat.
     *
     * @return the new AnimalCat instance
     */
    public AnimalCat build() {
      return new AnimalCat(this);
    }

    /**
     * Set the animalType.
     *
     * @param animalType the animalType
     * @return the AnimalCat builder
     */
    public Builder animalType(String animalType) {
      this.animalType = animalType;
      return this;
    }

    /**
     * Set the color.
     *
     * @param color the color
     * @return the AnimalCat builder
     */
    public Builder color(String color) {
      this.color = color;
      return this;
    }

    /**
     * Add an arbitrary property.
     *
     * @param name the name of the property to add
     * @param value the value of the property to add
     * @return the AnimalCat builder
     */
    public Builder add(String name, String value) {
      com.ibm.cloud.sdk.core.util.Validator.notNull(name, "name cannot be null");
      if (this.dynamicProperties == null) {
        this.dynamicProperties = new HashMap<String, String>();
      }
      this.dynamicProperties.put(name, value);
      return this;
    }
  }

  protected AnimalCat(Builder builder) {
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.animalType,
      "animalType cannot be null");
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.color,
      "color cannot be null");
    animalType = builder.animalType;
    color = builder.color;
    this.setProperties(builder.dynamicProperties);
  }

  /**
   * New builder.
   *
   * @return a AnimalCat builder
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Sets the animalType.
   *
   * @param animalType the new animalType
   */
  public void setAnimalType(final String animalType) {
    this.animalType = animalType;
  }

  /**
   * Sets the color.
   *
   * @param color the new color
   */
  public void setColor(final String color) {
    this.color = color;
  }
}
