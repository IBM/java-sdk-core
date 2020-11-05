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
import java.util.Date;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This class will serialize a java.util.Date instance associated with an OpenAPI "date-time" schema property.
 * The "deserialize" method is inherited from the DateDeserializer class.
 */
public class OpenAPIDateTimeSerializer extends DateDeserializer implements JsonSerializer<Date> {

  /*
   * (non-Javadoc)
   *
   * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type,
   * com.google.gson.JsonSerializationContext)
   */
  @Override
  public synchronized JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
    return src == null ? JsonNull.INSTANCE : new JsonPrimitive(DateUtils.formatAsDateTime(src));
  }
}
