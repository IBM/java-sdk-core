package com.ibm.cloud.sdk.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BooleanToStringTypeAdapterTest {

  private Gson gson;

  @Test
  public void writeShouldConvertBooleanTrueToStringYes() {
    TestData trueToYes = new TestData();
    trueToYes.setBooleanValue(true);
    String trueToYesResult = gson.toJson(trueToYes);
    assertTrue(trueToYesResult.contains("\"boolean_value\":\"yes\""));
  }

  @Test
  public void writeShouldConvertBooleanFalseToStringNo() {
    TestData falseToNo = new TestData();
    falseToNo.setBooleanValue(false);
    String falseToNoResult = gson.toJson(falseToNo);
    assertTrue(falseToNoResult.contains("\"boolean_value\":\"no\""));
  }

  @Test
  public void writeShouldRemoveNodeWhenBooleanIsNull() {
    TestData nullToNull = new TestData();
    nullToNull.setBooleanValue(null);
    nullToNull.setName("name");
    String nullToNullResult = gson.toJson(nullToNull);
    assertFalse(nullToNullResult.contains("\"boolean_value\""));
    assertTrue(nullToNullResult.contains("\"name\":\"name\""));
  }

  @Test
  public void readShouldConvertStringYesToBooleanTrue() {
    String yesToTrue = "{\"boolean_value\":\"yes\"}";
    TestData yesToTrueResult = gson.fromJson(yesToTrue, TestData.class);
    assertTrue(yesToTrueResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertStringNoToBooleanFalse() {
    String noToFalse = "{\"boolean_value\":\"no\"}";
    TestData noToFalseResult = gson.fromJson(noToFalse, TestData.class);
    assertFalse(noToFalseResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertStringTrueToBooleanTrue() {
    String trueToTrueData = "{\"boolean_value\":\"true\"}";
    TestData trueToTrueResult = gson.fromJson(trueToTrueData, TestData.class);
    assertTrue(trueToTrueResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertStringFalseToBooleanFalse() {
    String falseToFalseData = "{\"boolean_value\":\"false\"}";
    TestData falseToFalseResult = gson.fromJson(falseToFalseData, TestData.class);
    assertFalse(falseToFalseResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertStringNullToBooleanNull() {
    String nullToNullData = "{\"boolean_value\":null, \"name\":\"Andras\"}";
    TestData nullToNullResult = gson.fromJson(nullToNullData, TestData.class);
    assertNull(nullToNullResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertEmptyToBooleanNull() {
    String emptyToNullData = "{\"boolean_value\":\"\", \"name\":\"Andras\"}";
    TestData emptyToNullResult = gson.fromJson(emptyToNullData, TestData.class);
    assertNull(emptyToNullResult.getBooleanValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void readShouldThrowWhenValueCannotBeConvertToBoolean() {
    String emptyToNullData = "{\"boolean_value\":\"invalid_value\", \"name\":\"Andras\"}";
    TestData emptyToNullResult = gson.fromJson(emptyToNullData, TestData.class);
  }

  @Before
  public void before() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Boolean.class, new BooleanToStringTypeAdapter());
    gson = gsonBuilder.create();
  }

  private class TestData extends GenericModel {

    @SerializedName("boolean_value")
    private Boolean booleanValue;

    @SerializedName("name")
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

  }

}
