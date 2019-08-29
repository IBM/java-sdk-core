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

package com.ibm.cloud.sdk.core.service.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

/**
 * Abstract model class for a model which supports dynamic (arbitrary) properties of type T.
 */
public abstract class DynamicModel<T> implements ObjectModel {
  private TypeToken<T> additionalPropertyTypeToken;

  // The set of dynamic properties associated with this object.
  private Map<String, T> dynamicProperties = new HashMap<>();

  /**
   * Force use of 1-arg ctor.
   */
  @SuppressWarnings("unused")
  private DynamicModel() {
  }

  /**
   * This ctor accepts a TypeToken instance that represents the type of values stored in the map.
   *
   * @param t
   *          the TypeToken which represents the type of map values
   */
  public DynamicModel(TypeToken<T> t) {
    this.additionalPropertyTypeToken = t;
  }

  /**
   * Returns the TypeToken which describes the type of additional properties stored in the map.
   * @return The TypeToken which describes the map value type
   */
  public TypeToken<T> getAdditionalPropertyTypeToken() {
    return this.additionalPropertyTypeToken;
  }

  /**
   * Sets an arbitrary property.
   *
   * @param key
   *          the name of the property to set
   * @param value
   *          the value of the property to be set
   * @return the previous value of the property, or null if the property was not previously set
   */
  public T put(String key, T value) {
    return this.dynamicProperties.put(key, value);
  }

  /**
   * Returns the value of the specified property.
   *
   * @param key
   *          the name of the property to get
   * @return the value of the property, or null if the property is not set
   */
  public T get(String key) {
    return this.dynamicProperties.get(key);
  }

  /**
   * Returns a map containing the arbitrary properties set on this object.
   *
   * @return a copy of the map containing arbitrary properties set on this object
   */
  public Map<String, T> getProperties() {
    return new HashMap<String, T>(this.dynamicProperties);
  }

  /**
   * Returns the names of arbitrary properties set on this object.
   * @return a set containing the names of arbitrary properties set on this object
   */
  public Set<String> getPropertyNames() {
    return this.dynamicProperties.keySet();
  }
  /**
   * Removes the specified property from this object's map of arbitrary properties.
   *
   * @param key
   *          the name of the property to be removed
   * @return the previous value of the property, or null if the property was not previously set
   */
  public T removeProperty(String key) {
    return this.dynamicProperties.remove(key);
  }

  /**
   * Removes all of the arbitrary properties set on this object.
   */
  public void removeProperties() {
    this.dynamicProperties.clear();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final DynamicModel<?> other = (DynamicModel<?>) o;

    return toString().equals(other.toString());
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return GsonSingleton.getGson().toJson(this);
  }
}
