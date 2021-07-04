package com.ibm.cloud.sdk.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanToStringTypeAdapterTest {

  private Gson gson;

  @Test
  public void writeShouldConvertBooleanToString() {

    Model trueToYesModel = new Model();
    trueToYesModel.setBooleanValue(true);
    String trueToYesResult = gson.toJson(trueToYesModel);
    assertTrue(trueToYesResult.contains("\"booleanValue\":\"yes\""));

    Model falseToNoModel = new Model();
    falseToNoModel.setBooleanValue(false);
    String falseToNoResult = gson.toJson(falseToNoModel);
    assertTrue(falseToNoResult.contains("\"booleanValue\":\"no\""));
  }

  @Test
  public void readShouldConvertStringToBoolean() {
    String yesToTrueData = "{\"booleanValue\":\"yes\"}";
    Model yesToTrueResult = gson.fromJson(yesToTrueData, Model.class);
    assertTrue(yesToTrueResult.getBooleanValue());

    String noToFalseData = "{\"booleanValue\":\"no\"}";
    Model noToFalseResult = gson.fromJson(noToFalseData, Model.class);
    assertFalse(noToFalseResult.getBooleanValue());
  }

  @Test
  public void readShouldConvertBooleanToBoolean() {
    String trueToTrueData = "{\"booleanValue\":\"true\"}";
    Model trueToTrueResult = gson.fromJson(trueToTrueData, Model.class);
    assertTrue(trueToTrueResult.getBooleanValue());

    String falseToFalseData = "{\"booleanValue\":\"false\"}";
    Model falseToFalseResult = gson.fromJson(falseToFalseData, Model.class);
    assertFalse(falseToFalseResult.getBooleanValue());
  }

  @Before
  public void before() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Boolean.class, new BooleanToStringTypeAdapter());
    gson = gsonBuilder.create();
  }

  private class Model {

    private Boolean booleanValue;

    public Boolean getBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

  }

}
