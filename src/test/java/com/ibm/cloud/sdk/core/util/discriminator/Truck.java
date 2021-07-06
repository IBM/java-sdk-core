package com.ibm.cloud.sdk.core.util.discriminator;

import com.google.gson.annotations.SerializedName;

public class Truck {

  @SerializedName("vehicle_type")
  private String vehicleType = "truck";
  private int maxLoad = 40;

  public String getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(String vehicleType) {
    this.vehicleType = vehicleType;
  }

  public int getMaxLoad() {
    return maxLoad;
  }

  public void setMaxLoad(Integer maxLoad) {
    this.maxLoad = maxLoad;
  }

}
