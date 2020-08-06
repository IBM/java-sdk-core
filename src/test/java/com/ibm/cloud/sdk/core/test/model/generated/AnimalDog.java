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
 * Information about a dog.
 */
public class AnimalDog extends Animal {


  public AnimalDog() {
    super();
  }

  /**
   * Builder.
   */
  public static class Builder {
    private String animalType;
    private String breed;
    private Map<String, String> dynamicProperties;

    public Builder(Animal animalDog) {
      this.animalType = animalDog.animalType;
      this.breed = animalDog.breed;
      this.dynamicProperties = animalDog.getProperties();
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
     * @param breed the breed
     */
    public Builder(String animalType, String breed) {
      this.animalType = animalType;
      this.breed = breed;
    }

    /**
     * Builds a AnimalDog.
     *
     * @return the new AnimalDog instance
     */
    public AnimalDog build() {
      return new AnimalDog(this);
    }

    /**
     * Set the animalType.
     *
     * @param animalType the animalType
     * @return the AnimalDog builder
     */
    public Builder animalType(String animalType) {
      this.animalType = animalType;
      return this;
    }

    /**
     * Set the breed.
     *
     * @param breed the breed
     * @return the AnimalDog builder
     */
    public Builder breed(String breed) {
      this.breed = breed;
      return this;
    }

    /**
     * Add an arbitrary property.
     *
     * @param name the name of the property to add
     * @param value the value of the property to add
     * @return the AnimalDog builder
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

  protected AnimalDog(Builder builder) {
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.animalType,
      "animalType cannot be null");
    com.ibm.cloud.sdk.core.util.Validator.notNull(builder.breed,
      "breed cannot be null");
    animalType = builder.animalType;
    breed = builder.breed;
    this.setProperties(builder.dynamicProperties);
  }

  /**
   * New builder.
   *
   * @return a AnimalDog builder
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
   * Sets the breed.
   *
   * @param breed the new breed
   */
  public void setBreed(final String breed) {
    this.breed = breed;
  }
}
