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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.reflect.ReflectionAccessor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ibm.cloud.sdk.core.service.model.DynamicModel;

/**
 * This class is registered with Gson to perform serialization and deserialization for dynamic model classes.
 * Dynamic model classes are generated for JSON schemas that have the <code>additionalProperties</code> property set.
 *
 * <p>These generated classes will extend DynamicModel&lt;T&gt;, where T represents the type of values that
 * can be stored as additional (arbitrary) properties.
 * <br>Note that T could be:
 * <ul>
 * <li><code>Object</code></li>
 * <li><code>String</code></li>
 * <li><code>Long</code></li>
 * <li>etc.</li>
 * </ul>
 * OR it could be a user-defined type (i.e. MyModel).
 *
 * <p>Each generated dynamic model class will have zero or more explicitly-defined fields which model the schema
 * properties found in the corresponding JSON schema, plus a map (inherited from DynamicModel) containing
 * additional arbitrary properties.
 *
 * <p>Limitations:
 * <br>Note that this type adapter is not a "full-function" type adapter like the internal Gson-provided ones.
 * Specifically, this type adapter does not support all of the inclusion and exclusion options
 * related to the serialization and deserialization of fields.  Instead, each field defined on the class must
 * be annotated with Gson's <code>SerializedName</code> annotation for it to be included in the serialization process.
 *
 * <p>This class includes code that was adapted from the internal <code>ReflectiveTypeAdapterFactory</code> and
 * <code>MapTypeAdapterFactory</code> classes from Gson.
 */
public class DynamicModelTypeAdapterFactory implements TypeAdapterFactory {
  private static final Logger LOGGER = Logger.getLogger(DynamicModelTypeAdapterFactory.class.getName());
  private ReflectionAccessor accessor = ReflectionAccessor.getInstance();

  public DynamicModelTypeAdapterFactory() {
  }

  // If "type" represents a class that is recognized as an instance of DynamicModel AND it has a public
  // default ctor, this method will return a new TypeAdapter to handle the Gson serialization and deserialization of
  // it, otherwise null is returned.
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

    // Grab the Class associated with the instance to be serialized/deserialized.
    Class<? super T> rawType = type.getRawType();
    LOGGER.fine(this.getClass().getSimpleName() + " examining class: " + rawType.getName());

    // If "type" represents a type OTHER THAN a DynamicModel subclass, then bail out now.
    if (!DynamicModel.class.isAssignableFrom(rawType)) {
      LOGGER.fine("Class '" + rawType.getName() + "' is not a DynamicModel.");
      return null;
    }

    // Retrieve the type's default ctor. If one is not present, then bail out now.
    Constructor<?> ctor = getDefaultCtor(rawType);
    if (ctor == null) {
      LOGGER.warning("Instance of class " + rawType.getName() + " is a subclass of DynamicModel, but it doesn't "
        + "define a default constructor.  This instance will be ignored by " + this.getClass().getSimpleName());
      return null;
    }

    LOGGER.fine("Returning TypeAdapter instance to handle class: " + rawType.getName());
    return new Adapter<T>(gson, ctor, getBoundFields(gson, type));
  }

  /**
   * Returns the no-arg (default) ctor associated with the Class "clazz".
   *
   * @param clazz
   *          the class whose default ctor should be retrieved
   * @return clazz's default ctor
   */
  protected Constructor<?> getDefaultCtor(Class<?> clazz) {
    Constructor<?>[] allCtors = clazz.getDeclaredConstructors();
    for (int i = 0; i < allCtors.length; i++) {
      Constructor<?> ctor = allCtors[i];
      if (ctor.getParameterTypes().length == 0) {
        if (!ctor.isAccessible()) {
          accessor.makeAccessible(ctor);
        }
        return ctor;
      }
    }
    return null;
  }

  /**
   * Returns a map (keyed by field name) of BoundField objects which represent the fields found within the specified
   * type and all its super types.
   * @param context the Gson object
   * @param type the TypeToken object that represents the class being examined
   * @return a map containing the set of fields to be included in ser/deser operations
   */
  protected Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type) {
    Map<String, BoundField> result = new LinkedHashMap<String, BoundField>();
    Class<?> raw = type.getRawType();
    if (raw.isInterface()) {
      return result;
    }

    // Walk the type hierarchy starting at the class represented by "type".
    Type declaredType = type.getType();
    while (raw != Object.class) {
      Field[] fields = raw.getDeclaredFields();
      for (Field field : fields) {
        List<String> fieldNames = getFieldNames(field);
        if (fieldNames == null) {
          continue;
        }

        // We'll assume that each field should be serialized and de-serialized
        // unless/until we know otherwise.
        boolean serialize = true;
        boolean deserialize = true;

        accessor.makeAccessible(field);

        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
        BoundField previous = null;
        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
          String name = fieldNames.get(i);

          // Serialize using only the default field name.
          if (i != 0) {
            serialize = false;
          }
          BoundField boundField =
              createBoundField(context, field, name, TypeToken.get(fieldType), serialize, deserialize);
          BoundField replaced = result.put(name, boundField);
          if (previous == null)
            previous = replaced;
        }
        if (previous != null) {
          throw new IllegalArgumentException(declaredType + " declares multiple JSON fields named " + previous.name);
        }
      }

      // Now walk up the hierarchy one level to the current type's super class.
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return result;
  }

  /**
   * Returns a list of field names for the specified Field. Note that we will return field names ONLY if the field has
   * the SerializedName annotation set on it.
   *
   * @param f
   *          the field
   * @return a list of field names associated with the Field, with the first field name representing the "default" name
   *         used in serialization
   */
  private List<String> getFieldNames(Field f) {
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      return null;
    }

    String serializedName = annotation.value();
    String[] alternates = annotation.alternate();
    if (alternates.length == 0) {
      return Collections.singletonList(serializedName);
    }

    List<String> fieldNames = new ArrayList<String>(alternates.length + 1);
    fieldNames.add(serializedName);
    for (String alternate : alternates) {
      fieldNames.add(alternate);
    }
    return fieldNames;
  }

  /**
   * Abstract base class used to represent bound fields.
   */
  abstract static class BoundField {
    final String name;
    final boolean serialized;
    final boolean deserialized;

    protected BoundField(String name, boolean serialized, boolean deserialized) {
      this.name = name;
      this.serialized = serialized;
      this.deserialized = deserialized;
    }

    abstract boolean writeField(Object value) throws IOException, IllegalAccessException;

    abstract void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException;

    abstract void read(JsonReader reader, Object value) throws IOException, IllegalAccessException;
  }

  /**
   * Creates a BoundField instance for the specified Field.
   * @param context the Gson context
   * @param field the java Field that we'll create a BoundField instance for
   * @param name the name associated with the Field
   * @param fieldType a TypeToken that describes the type of the Field
   * @param serialize a flag indicating whether the field should be included in serialization operations
   * @param deserialize a flag indicating whether the field should be included in deserialization operations
   * @return the BoundField representing the Field
   */
  protected DynamicModelTypeAdapterFactory.BoundField createBoundField(final Gson context, final Field field,
    final String name, final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
    final TypeAdapter<?> typeAdapter = context.getAdapter(fieldType);

    return new DynamicModelTypeAdapterFactory.BoundField(name, serialize, deserialize) {
      @SuppressWarnings({
          "unchecked", "rawtypes"
      })
      @Override
      void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
        Object fieldValue = field.get(value);
        TypeAdapter t = new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
        t.write(writer, fieldValue);
      }

      @Override
      void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          field.set(value, fieldValue);
        }
      }

      @Override
      public boolean writeField(Object value) throws IOException, IllegalAccessException {
        if (!serialized) {
          return false;
        }
        Object fieldValue = field.get(value);
        return fieldValue != value; // avoid recursion for example for Throwable.cause
      }
    };
  }

  /**
   * An adapter for serializing/deserializing instances of type T, where T represents a generated dynamic model class
   * which is a subclass of DynamicModel. Subclasses of DynamicModel will have zero or more explicitly-defined fields
   * plus a map to store additional (arbitrary) properties.
   */
  public static class Adapter<T> extends TypeAdapter<T> {
    private Constructor<?> ctor;
    private Map<String, BoundField> boundFields;
    private Gson gson;
    private TypeAdapter<?> mapValueObjectTypeAdapter;

    Adapter(Gson gson, Constructor<?> ctor, Map<String, BoundField> boundFields) {
      this.gson = gson;
      this.ctor = ctor;
      this.boundFields = boundFields;
      this.mapValueObjectTypeAdapter = new MapValueObjectTypeAdapter(gson);
    }

    /*
     * (non-Javadoc)
     * @see com.google.gson.TypeAdapter#write(com.google.gson.stream.JsonWriter, java.lang.Object)
     */
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      try {
        // First, serialize each of the bound fields.
        for (BoundField boundField : boundFields.values()) {
          if (boundField.writeField(value)) {
            out.name(boundField.name);
            boundField.write(out, value);
          }
        }

        // Next, we need to dynamically retrieve the additionalPropertyTypeToken field
        // from the DynamicModel instance and retrieve its TypeAdapter for deserializing arbitrary properties.
        TypeToken<?> mapValueType = getMapValueType(value);
        TypeAdapter mapValueTypeAdapter = gson.getAdapter(mapValueType);

        // If the map value type is Object, then we need to use our own flavor of the Gson ObjectTypeAdapter.
        if (mapValueType.getRawType().equals(Object.class)) {
          mapValueTypeAdapter = this.mapValueObjectTypeAdapter;
        } else {
          mapValueTypeAdapter = gson.getAdapter(mapValueType);
        }

        // Next, serialize each of the map entries.
        for (String key : ((DynamicModel<?>) value).getPropertyNames()) {
          out.name(String.valueOf(key));
          mapValueTypeAdapter.write(out, ((DynamicModel<?>) value).get(key));
        }
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
      out.endObject();
    }

    /*
     * (non-Javadoc)
     * @see com.google.gson.TypeAdapter#read(com.google.gson.stream.JsonReader)
     */
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      // Create a new instance of the class to be deserialized.
      T instance;
      try {
        instance = (T) ctor.newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        throw new IOException("Could not instantiate class: " + ctor.getDeclaringClass().getName(), e);
      }

      // Next, we need to dynamically retrieve the additionalPropertyTypeToken field
      // from the DynamicModel instance and retrieve its TypeAdapter for deserializing arbitrary properties.
      TypeToken<?> mapValueType = getMapValueType(instance);
      TypeAdapter<?> mapValueTypeAdapter;

      // If the map value type is Object, then we need to use our own flavor of the Gson ObjectTypeAdapter.
      if (mapValueType.getRawType().equals(Object.class)) {
        mapValueTypeAdapter = this.mapValueObjectTypeAdapter;
      } else {
        mapValueTypeAdapter = gson.getAdapter(mapValueType);
      }

      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();
          BoundField field = boundFields.get(name);

          // If we found a BoundField for <name>, then it must be an explicitly-defined model property.
          if (field != null) {
            if (field.deserialized) {
              field.read(in, instance);
            } else {
              // field is set to NOT deserialize, so skip it.
              in.skipValue();
            }
          } else {
            // Otherwise, it must be an additional property so it belongs in the map.
            String key = name;
            Object value = mapValueTypeAdapter.read(in);

            // Now add the arbitrary property/value to the map.
            Object replaced = ((DynamicModel) instance).put(key, value);

            // If this new map entry is replacing an existing entry, then we must have a duplicate.
            if (replaced != null) {
              throw new JsonSyntaxException("Duplicate key: " + key);
            }
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
      in.endObject();
      return instance;
    }

    /**
     * Returns a TypeToken which represents the type of values stored in the DynamicModel's inherited map.
     * @param instance the DynamicModel instance from which we'll retrieve the TypeToken
     * @return the TypeToken representing the map value type
     */
    public TypeToken<?> getMapValueType(T instance) {
      TypeToken<?> result = ((DynamicModel<?>) instance).getAdditionalPropertyTypeToken();
      // If we can't retrieve the map value TypeToken from the instance, then default to Object.class;
      if (result == null) {
        result = TypeToken.get(Object.class);
      }
      return result;
    }
  }
}
