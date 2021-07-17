package com.ibm.cloud.sdk.core.util;

import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LongToDateTimeAdapterTest {
  private Gson gson;

  @Test
  public void writeShouldSerializeDateNullToJsonNullValue() {
    // Arrange
    LongToDateTimeAdatpterModel model = new LongToDateTimeAdatpterModel();
    model.birthDate = null;
    model.name = "Lorem";

    // Act
    String serialized = gson.toJson(model);

    // Assert
    assertTrue(serialized.contains("\"name\":\"Lorem\""));
    assertFalse(serialized.contains("birth_date"));
  }

  @BeforeMethod
  public void beforeMethod() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Date.class, new LongToDateTypeAdapter());
    gson = gsonBuilder.create();
  }

  private class LongToDateTimeAdatpterModel {
    @SerializedName("birth_date")
    public Date birthDate;

    @SerializedName("name")
    public String name;

  }
}
