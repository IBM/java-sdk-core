/**
 * (C) Copyright IBM Corp. 2019.
 * Copyright (C) 2011 Google Inc.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * This class is registered with Gson to perform deserialization of classes that have discriminator metadata
 * defined in them.   This would result from an OpenAPI schema that defines a "discriminator" object to aid
 * in the proper type selection during deserialization.
 *
 * Here is an example of a class that contains the discriminator metdata:
 *
 * public class Vehicle extends GenericModel {
 *   private static String discriminatorPropertyName = "vehicle_type";
 *   private static java.util.Map&lt;String, Class&lt;?&gt;&gt; discriminatorMapping;
 *   static {
 *     discriminatorMapping = new java.util.HashMap&lt;&gt;();
 *     discriminatorMapping.put("Truck", Truck.class);
 *     discriminatorMapping.put("Car", Car.class);
 *   }
 * }
 *
 * In this example, we'd expect the JSON object to contain the "vehicle_type" field.  If the field's value is
 * "Truck", then we should deserialize the object into an instance of the Truck class, and if the field's value is
 * "Car", then we should deserialize the object into an instance of the Car class.
 *
 * This factory's 'create' method will examine the Class object passed in to determine if it contains
 * the discriminator metadata.  If it does, then we'll construct a new TypeAdapter for the specific class and return it
 * to Gson where it will be cached and used for any subsequent instances of the same class.
 * Otherwise, we'll return null to indicate that this factory shouldn't be used to handle the specified class.
 */
public class DiscriminatorBasedTypeAdapterFactory implements TypeAdapterFactory {
  private static final Logger LOGGER = Logger.getLogger(DiscriminatorBasedTypeAdapterFactory.class.getName());

  private static final String DISC_PROPERTY_NAME_FIELD = "discriminatorPropertyName";
  private static final String DISC_MAPPING_FIELD = "discriminatorMapping";

  public DiscriminatorBasedTypeAdapterFactory() {
  }

  /**
   * This method will return a TypeAdapter instance if the specified type is found to contain the expected
   * discriminator metdata, or null otherwise.
   * We use reflection to scrape the discriminator information (property name and mapping data) from the Class,
   * then cache that information in a DiscriminatorMetadata instance which is then used to construct the TypeAdapter.
   * The TypeAdapter will then cache the DiscriminatorMetadata instance and use it for subsequent deserialization
   * operations.   Note that the TypeAdapter instance we return is bound to the specific Class described by the 'type'
   * parameter.
   *
   * @param gson the Gson instance
   * @param type a TypeToken instance that contains the Class to be examined
   * @return a TypeAdapter instance for the specified class or null
   */
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    LOGGER.fine(this.getClass().getSimpleName() + " examining class: " + rawType.getName());

    DiscriminatorMetadata discMetadata = getDiscriminatorMetadata(rawType);
    if (discMetadata != null) {
      LOGGER.fine("Returning TypeAdapter instance to handle class: " + rawType.getName());
      return new Adapter<T>(gson, discMetadata);
    }

    LOGGER.fine("Discriminator metadata not found in class: " + rawType.getName());
    return null;
  }

  /**
   * This class holds the discriminator-related metadata that is retrieved from a particular class.
   */
  static class DiscriminatorMetadata {
    // The Class that contains the discriminator metadata.
    private Class<?> discriminatorClass;

    // The name of the property (field) that we should expect to find in JSON objects being deserialized.
    private String propertyName;

    // The mapping info that indicates the deserialization target class for expected discriminator values.
    // We don't require "default" mappings to be explicitly specified.
    private Map<String, Class<?>> mapping;


    DiscriminatorMetadata(Class<?> discriminatorClass, String propertyName, Map<String, Class<?>> mapping) {
      this.discriminatorClass = discriminatorClass;
      this.propertyName = propertyName;
      this.mapping = mapping;
    }

    Class<?> getDiscriminatorClass() {
      return discriminatorClass;
    }

    String getPropertyName() {
      return propertyName;
    }

    Class<?> getSubclassMapping(String discriminatorValue) {
      return mapping != null ? mapping.get(discriminatorValue) : null;
    }
  }

  /**
   * Retrieves the discriminator metadata from the specified Class, if present.
   * @param clazz the Class that potentially contains the discriminator metadata
   * @return a DiscriminatorMetadata instance or null if no metadata was found on the class
   */
  @SuppressWarnings("unchecked")
  private DiscriminatorMetadata getDiscriminatorMetadata(Class<?> clazz) {
    try {
      String propName = null;
      Map<String, Class<?>> mapping = null;
      Field propNameField = clazz.getDeclaredField(DISC_PROPERTY_NAME_FIELD);
      if (propNameField != null) {
        propNameField.setAccessible(true);
        propName = (String) propNameField.get(null);
      }

      Field mappingField = clazz.getDeclaredField(DISC_MAPPING_FIELD);
      if (mappingField != null) {
        mappingField.setAccessible(true);
        mapping = (Map<String, Class<?>>) mappingField.get(null);
      }

      if (propName != null && mapping != null) {
        return new DiscriminatorMetadata(clazz, propName, mapping);
      }
    } catch (Throwable t) {
      // Any exception will cause us to just return null below.
    }

    return null;
  }

  /**
   * An adapter for serializing/deserializing instances of type T, where T represents a generated model
   * that defines a discriminator to aid in deserialization.
   * This TypeAdapter will use the cached DiscriminatorMetadata to select the proper Class
   * for the deserialization target.
   */
  public static class Adapter<T> extends TypeAdapter<T> {
    private Gson gson;
    private DiscriminatorMetadata discMetadata;

    Adapter(Gson gson, DiscriminatorMetadata discMetadata) {
      this.gson = gson;
      this.discMetadata = discMetadata;
    }

    /**
     * We don't actually need to support the serialization of a class that contains the discriminator metadata
     * because we should encounter only instances of its subclasses, as opposed to instances of the base class
     * itself.
     */
    @Override
    public void write(JsonWriter out, T value) throws IOException {
      // We should never be asked to serialize a generated class that contains
      // the discriminator metadata, but just in case, just throw an exception.
      throw new IOException("Serialization of discriminator base classes is not supported");
    }

    /**
     * This method is responsible for deserializing a JSON object into an instance of the class
     * identified by the discriminator mapping information.
     * We'll perform the following steps:
     * 1) Do a parse of the JSON object to obtain the JsonElement parse tree.  This step produces an intermediate
     * form of the deserialized JSON object and consumes the entire object within the JsonReader.
     * 2) Retrieve the discriminator value from the JsonElement parse tree.
     * 3) Select the proper deserialization target class by looking up the discriminator value in the cached
     * mapping info.
     * 4) Deserialize the JsonElement parse tree into an instance of the selected deserialization target class.
     */
    @Override
    public T read(JsonReader in) throws IOException {
      LOGGER.finest("Attempting to deserialize JSON object for discriminator class: "
        + discMetadata.getDiscriminatorClass().getName());

      // If the "next" token is a null, then consume it and just return null now.
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      try {
        // 1) Parse the JSON object to produce the JsonElement parse tree.
        JsonParser parser = new JsonParser();
        JsonElement parseTree = parser.parse(in);
        LOGGER.finest("Parsed JSON into JsonElement tree: " + parseTree.toString());

        // 2 & 3) Determine the deserialization target class by retrieving the
        // discriminator value from the JsonElement parse tree, then look that value up
        // in the mapping info.
        Class<? extends T> deserTargetClass = getDeserTargetClass(parseTree);
        LOGGER.finest("Deserialization target class: " + deserTargetClass.getName());

        // 4) Finally, tell Gson to deserialize the JsonElement parse tree into the
        // deserialization target class.
        T instance = gson.fromJson(parseTree, deserTargetClass);

        return instance;
      } catch (Throwable t) {
        throw new IOException("The following error occurred while deserializing JSON object into discriminator class: "
            + discMetadata.getDiscriminatorClass().getName(), t);
      }
    }

    /**
     * Determine the correct Class object to serve as the deserialization target.
     * @param jsonTree the JSON parse tree associated with the JSON object to be deserialized
     * @return the Class representing the deserialization target
     */
    @SuppressWarnings("unchecked")
    private Class<? extends T> getDeserTargetClass(JsonElement jsonTree) throws IOException {
      // Make sure that the root JsonElement represents an object (as opposed to a primitive).
      if (!jsonTree.isJsonObject()) {
        throw new IOException("Parsed JSON is expected to be a JSON Object");
      }

      // Get the JsonObject represenation of the JSON parse tree.
      JsonObject jsonRoot = jsonTree.getAsJsonObject();

      // Find the discriminator property and retrieve it's value.
      JsonElement discProperty = jsonRoot.get(discMetadata.getPropertyName());
      if (discProperty == null) {
        throw new IOException("Required discriminator property '" + discMetadata.getPropertyName()
          + "' not found in JSON object");
      }

      String discValue = discProperty.getAsString();
      if (StringUtils.isEmpty(discValue)) {
        throw new IOException("Unable to retrieve discriminator value for property '"
          + discMetadata.getPropertyName() + "'");
      }

      // Lookup the discriminator value in the mapping Map to determine the deserialization target class.
      Class<? extends T> deserTargetClass = (Class<? extends T>) discMetadata.getSubclassMapping(discValue);

      // If we don't find an explicit mapping for the discriminator value, then we'll try to synthesize one
      // by using the package name of the discriminator class together with the discriminator value.
      // If this results in a class that extends the discriminator-containing class, then we're good.
      if (deserTargetClass == null) {
        String deserTargetName = discMetadata.getDiscriminatorClass().getPackage().getName() + "." + discValue;
        LOGGER.finest("Explicit discriminator mapping not found for value: " + discValue);

        try {
          deserTargetClass = (Class<? extends T>) Class.forName(deserTargetName);
          LOGGER.finest("Found implicit deserialization target class: " + deserTargetName);
        } catch (Throwable t) {
          throw new IOException("Unable to determine implicit deserialization target class for discriminator value: "
            + discValue);
        }
      }

      return deserTargetClass;
    }
  }
}
