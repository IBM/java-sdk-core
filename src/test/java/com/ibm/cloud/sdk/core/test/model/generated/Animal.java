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
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.service.model.DynamicModel;

/**
 * Generic information about an animal.
 *
 * Classes which extend this class:
 * - AnimalDog
 * - AnimalCat
 * - AnimalIguana
 */
public class Animal extends DynamicModel<String> {
  @SuppressWarnings("unused")
  protected static String discriminatorPropertyName = "animal_type";
  protected static java.util.Map<String, Class<?>> discriminatorMapping;
  static {
    discriminatorMapping = new java.util.HashMap<>();
    discriminatorMapping.put("dog", AnimalDog.class);
    discriminatorMapping.put("canine", AnimalDog.class);
    discriminatorMapping.put("cat", AnimalCat.class);
    discriminatorMapping.put("feline", AnimalCat.class);
    discriminatorMapping.put("Iguana", AnimalIguana.class);
  }

  @SerializedName("animal_type")
  protected String animalType;
  @SerializedName("breed")
  protected String breed;
  @SerializedName("color")
  protected String color;
  @SerializedName("tail_length")
  protected Long tailLength;

  protected Animal() {
    super(new TypeToken<String>() { });
  }

  /**
   * Gets the animalType.
   *
   * @return the animalType
   */
  public String getAnimalType() {
    return this.animalType;
  }

  /**
   * Gets the breed.
   *
   * @return the breed
   */
  public String getBreed() {
    return this.breed;
  }

  /**
   * Gets the color.
   *
   * @return the color
   */
  public String getColor() {
    return this.color;
  }

  /**
   * Gets the tailLength.
   *
   * @return the tailLength
   */
  public Long getTailLength() {
    return this.tailLength;
  }
}
