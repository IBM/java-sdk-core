/*
 * (C) Copyright IBM Corp. 2020.
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
 * Information about an iguana.
 */
public class AnimalIguana extends Animal {


  public AnimalIguana() {
    super();
  }

  /**
   * Builder.
   */
  public static class Builder {
    private String animalType;
    private Long tailLength;
    private Map<String, String> dynamicProperties;

    public Builder(Animal animalIguana) {
      this.animalType = animalIguana.animalType;
      this.tailLength = animalIguana.tailLength;
      this.dynamicProperties = animalIguana.getProperties();
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
     * @param tailLength the tailLength
     */
    public Builder(String animalType, Long tailLength) {
      this.animalType = animalType;
      this.tailLength = tailLength;
    }

    /**
     * Builds a AnimalIguana.
     *
     * @return the new AnimalIguana instance
     */
    public AnimalIguana build() {
      return new AnimalIguana(this);
    }

    /**
     * Set the animalType.
     *
     * @param animalType the animalType
     * @return the AnimalIguana builder
     */
    public Builder animalType(String animalType) {
      this.animalType = animalType;
      return this;
    }

    /**
     * Set the tailLength.
     *
     * @param tailLength the tailLength
     * @return the AnimalIguana builder
     */
    public Builder tailLength(long tailLength) {
      this.tailLength = tailLength;
      return this;
    }

    /**
     * Add an arbitrary property.
     *
     * @param name the name of the property to add
     * @param value the value of the property to add
     * @return the AnimalIguana builder
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

  protected AnimalIguana(Builder builder) {
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.animalType,
      "animalType cannot be null");
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.tailLength,
      "tailLength cannot be null");
    animalType = builder.animalType;
    tailLength = builder.tailLength;
    this.setProperties(builder.dynamicProperties);
  }

  /**
   * New builder.
   *
   * @return a AnimalIguana builder
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
   * Sets the tailLength.
   *
   * @param tailLength the new tailLength
   */
  public void setTailLength(final long tailLength) {
    this.tailLength = tailLength;
  }
}
