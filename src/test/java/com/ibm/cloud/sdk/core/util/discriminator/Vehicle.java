package com.ibm.cloud.sdk.core.util.discriminator;

import java.util.HashMap;
import java.util.Map;

public class Vehicle {
  private static String discriminatorPropertyName = "vehicle_type";
  private static Map<String, Class<?>> discriminatorMapping;
  static {
    discriminatorMapping = new HashMap<>();
    discriminatorMapping.put("Truck", Truck.class);
    discriminatorMapping.put("Car", Car.class);
  }
}
