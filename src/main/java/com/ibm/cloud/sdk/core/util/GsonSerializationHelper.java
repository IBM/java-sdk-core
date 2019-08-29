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

package com.ibm.cloud.sdk.core.util;

import java.lang.reflect.Type;

/**
 * Utility class to help with serialization in models which extend
 * {@link com.ibm.cloud.sdk.core.service.model.DynamicModel}.
 *
 * @see com.ibm.cloud.sdk.core.service.model.DynamicModel
 * @deprecated This class should no longer be needed once users upgrade to a recent version of
 * the package that contains the new DynamicModel pattern.
 */
@Deprecated
public class GsonSerializationHelper {

  private GsonSerializationHelper() {
    // This is a utility class - no instantiation allowed.
  }

  /**
   * Takes a property of an object extending {@link com.ibm.cloud.sdk.core.service.model.DynamicModel} and
   * serializes it to the desired type. Without this conversion, properties which also happen to
   * extend {@link com.ibm.cloud.sdk.core.service.model.DynamicModel} throw an exception when
   * trying to cast to their concrete type from the default Gson serialization.
   *
   * @param property property of a DynamicModel
   * @param type the type we wish to convert the property to
   * @param <T> the generic type
   * @return the properly converted object
   */
  public static <T> T serializeDynamicModelProperty(Object property, Type type) {
    return GsonSingleton.getGson().fromJson(GsonSingleton.getGson().toJsonTree(property), type);
  }
}
