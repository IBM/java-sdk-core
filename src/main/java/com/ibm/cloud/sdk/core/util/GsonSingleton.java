/**
 * (C) Copyright IBM Corp. 2015, 2024.
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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Gson singleton to be use when transforming from JSON to Java Objects and vise versa. It handles date formatting and
 * pretty print the result
 */
public final class GsonSingleton {
  private static final Logger LOG = Logger.getLogger(GsonSingleton.class.getName());

  private static Gson gson;
  private static Gson gsonWithoutPrinting;

  private GsonSingleton() {
    // This is a utility class - no instantiation allowed.
  }

  /**
   * Creates a {@link com.google.gson.Gson} object that can be use to serialize and deserialize Java objects.
   *
   * @param prettyPrint if true, the JSON will be pretty printed
   * @param serializeNulls if true, then null values will be serialized in the JSON
   * @return the {@link Gson}
   */
  private static Gson createGson(boolean prettyPrint, boolean serializeNulls) {
    LOG.log(Level.FINE, "Creating new Gson context; prettyPrint={0}, serializeNulls={1}",
        new Object[] { prettyPrint, serializeNulls });

    GsonBuilder builder = new GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
        .setNumberToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER);

    registerTypeAdapters(builder);

    if (prettyPrint) {
      builder.setPrettyPrinting();
    }
    if (serializeNulls) {
      builder.serializeNulls();
    }
    builder.disableHtmlEscaping();
    return builder.create();
  }

  private static void registerTypeAdapters(GsonBuilder builder) {
    // Date serializer/deserializer.
    // We treat Date's as date-time by default.
    builder.registerTypeAdapter(Date.class, new DateTimeTypeAdapter());
    LOG.log(Level.FINE, "Registered type adapter {0} for type {1}",
        new Object[] {DateTimeTypeAdapter.class.getSimpleName(), Date.class.getName()});

    // Make sure that byte[] ser/deser includes base64 encoding/decoding.
    builder.registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter());
    LOG.log(Level.FINE, "Registered type adapter {0} for type {1}",
        new Object[] {ByteArrayTypeAdapter.class.getSimpleName(), "byte[]"});

    // Make sure we serialize LazilyParsedNumber properly to avoid unnecessary decimal places in serialized integers.
    builder.registerTypeAdapter(LazilyParsedNumber.class, LAZILY_PARSED_NUMBER_ADAPTER);
    LOG.log(Level.FINE, "Registered type adapter {0} for type {1}",
        new Object[] {LAZILY_PARSED_NUMBER_ADAPTER.getClass().getName(), LazilyParsedNumber.class.getName()});

    // Type adapter factory for DynamicModel subclasses.
    builder.registerTypeAdapterFactory(new DynamicModelTypeAdapterFactory());
    LOG.log(Level.FINE, "Registered type adapter factory {0}", DynamicModelTypeAdapterFactory.class.getName());

    // Type adapter factory for classes that use a discriminator.
    builder.registerTypeAdapterFactory(new DiscriminatorBasedTypeAdapterFactory());
    LOG.log(Level.FINE, "Registered type adapter factory {0}", DiscriminatorBasedTypeAdapterFactory.class.getName());
  }

  /**
   * Gets the Gson instance.
   *
   * @return the Gson
   */
  public static synchronized Gson getGson() {
    if (gson == null) {
      gson = createGson(true, false);
    }
    return gson;
  }

  /**
   * Gets the Gson instance.
   *
   * @return the Gson
   */
  public static synchronized Gson getGsonWithoutPrettyPrinting() {
    if (gsonWithoutPrinting == null) {
      gsonWithoutPrinting = createGson(false, false);
    }
    return gsonWithoutPrinting;
  }

  /**
   * Returns an instance of Gson with the "serialize nulls" config option enabled.
   * @return a Gson instance configured to serialize nulls
   */
  public static Gson getGsonWithSerializeNulls() {
    return createGson(false, true);
  }


  public static final TypeAdapter<Number> LAZILY_PARSED_NUMBER_ADAPTER = new TypeAdapter<Number>() {
    @Override
    public Number read(JsonReader in) throws IOException {
      JsonToken jsonToken = in.peek();
      switch (jsonToken) {
      case NULL:
        in.nextNull();
        return null;
      case NUMBER:
      case STRING:
        return new LazilyParsedNumber(in.nextString());
      default:
        throw new JsonSyntaxException("Expecting number, got: " + jsonToken);
      }
    }
    @Override
    public void write(JsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

}
