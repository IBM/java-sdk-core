package com.ibm.cloud.sdk.core.util;

import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class LongToDateTimeAdapterTest {
  private Gson gson;
  private String name = "Lorem";

  @Test
  public void writeShouldSerializeDateNullValueToJsonNullValue() {
    // Arrange
    LongToDateTimeAdapterModel model = new LongToDateTimeAdapterModel();
    model.birthDate = null;
    model.name = name;

    // Act
    String serialized = gson.toJson(model);

    // Assert
    assertTrue(serialized.contains(String.format("\"name\":\"%s\"", name)));
    assertFalse(serialized.contains("birth_date"));
  }

  @Test
  public void writeShouldSerializeDateNowValueToJsonValue() {
    // Arrange
    Date date = new Date();
    LongToDateTimeAdapterModel model = new LongToDateTimeAdapterModel();
    model.birthDate = date;
    model.name = name;

    // Act
    String serialized = gson.toJson(model);

    // Assert
    assertTrue(serialized.contains(String.format("\"name\":\"%s\"", name)));
    assertTrue(serialized.contains(String.format("\"birth_date\":%d", date.getTime())));
  }

  @Test
  public void writeShouldSerializeDateWithPriorUnixTimeStartValueToJsonValue() {
    // Arrange
    LongToDateTimeAdapterModel model = new LongToDateTimeAdapterModel();
    model.birthDate = new Date(-31489139000L);
    model.name = name;

    // Act
    String serialized = gson.toJson(model);

    // Assert
    assertTrue(serialized.contains(String.format("\"name\":\"%s\"", name)));
    assertTrue(serialized.contains(String.format("\"birth_date\":%d", model.birthDate.getTime())));
  }

  @Test
  public void readShouldDeserializeJsonNullValueToDateNull() {
    // Arrange
    String json = String.format("{\"birth_date\":null, \"name\":\"%s\"}", name);

    // Act
    LongToDateTimeAdapterModel model = gson.fromJson(json, LongToDateTimeAdapterModel.class);

    // Assert
    assertNull(model.birthDate);
    assertEquals(model.name, name);
  }

  @Test(expectedExceptions = NumberFormatException.class)
  public void readShouldThrowWhenDateJsonValueIsEmptyString() {
    // Arrange
    String json = String.format("{\"birth_date\":\"\", \"name\":\"%s\"}", name);

    // Act
    LongToDateTimeAdapterModel model = gson.fromJson(json, LongToDateTimeAdapterModel.class);

    // Assert
    assertNull(model.birthDate);
    assertEquals(model.name, "Lorem");
  }

  @Test(expectedExceptions = NumberFormatException.class)
  public void readShouldThrowWhenJsonDateValueIsSpaceString() {
    // Arrange
    String json = String.format("{\"birth_date\":\" \", \"name\":\"%s\"}", name);

    // Act
    LongToDateTimeAdapterModel model = gson.fromJson(json, LongToDateTimeAdapterModel.class);

    // Assert
    assertNull(model.birthDate);
    assertEquals(model.name, "Lorem");
  }

  @Test
  public void readShouldDeserializeWhenJsonValueIsNumber() {
    // Arrange
    Date date = new Date();
    String json = String.format("{\"birth_date\":%d, \"name\":\"%s\"}", date.getTime(), name);

    // Act
    LongToDateTimeAdapterModel model = gson.fromJson(json, LongToDateTimeAdapterModel.class);

    // Assert
    assertNotNull(model.birthDate);
    assertEquals(model.birthDate.getTime(), date.getTime());
    assertEquals(model.name, "Lorem");
  }

  @Test
  public void readShouldDeserializeWhenJsonValueIsStringButNumberInIt() {
    // Arrange
    Date date = new Date();
    String json = String.format("{\"birth_date\":\"%d\", \"name\":\"%s\"}", date.getTime(), name);

    // Act
    LongToDateTimeAdapterModel model = gson.fromJson(json, LongToDateTimeAdapterModel.class);

    // Assert
    assertNotNull(model.birthDate);
    assertEquals(model.birthDate.getTime(), date.getTime());
    assertEquals(model.name, "Lorem");
  }


  @BeforeMethod
  public void beforeMethod() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Date.class, new LongToDateTypeAdapter());
    gson = gsonBuilder.create();
  }

  private class LongToDateTimeAdapterModel {
    @SerializedName("birth_date")
    public Date birthDate;

    @SerializedName("name")
    public String name;

  }
}
