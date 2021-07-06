package com.ibm.cloud.sdk.core.util.discriminator;

import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.util.GsonSingleton;
import org.junit.Before;
import org.junit.Test;

import static org.testng.Assert.assertEquals;

public class DiscriminatorBasedTypeAdapterFactoryTest {

  private Gson gson;

  @Before
  public void before() {
    gson = GsonSingleton.getGsonWithoutPrettyPrinting();
  }

  @Test
  public void deserialize() {
    String carJson = "{ \"vehicle_type\":\"Car\"}";
    Car carResult = gson.fromJson(carJson, Car.class);
    assertEquals(carResult.getVehicleType(), "Car");
    assertEquals(carResult.getConvertible(), Boolean.TRUE);

    String truckJson = "{ \"vehicle_type\":\"Truck\"}";
    Truck truckResult = gson.fromJson(truckJson, Truck.class);
    assertEquals(truckResult.getVehicleType(), "Truck");
    assertEquals(truckResult.getMaxLoad(), Integer.parseInt("40"));

    String test = "{ \"vehicle_type_type\":\"Truck\"}";
    Truck testResult = gson.fromJson(test, Truck.class);
    System.out.println("testresult: " + testResult);
    // assertEquals(testResult.getVehicleType(), "Truck");
    // assertEquals(testResult.getMaxLoad(), Integer.parseInt("40"));
  }

}
