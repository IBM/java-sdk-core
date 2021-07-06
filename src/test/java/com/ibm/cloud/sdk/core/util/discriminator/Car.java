package com.ibm.cloud.sdk.core.util.discriminator;

import com.google.gson.annotations.SerializedName;

public class Car {

  @SerializedName("vehicle_type")
  private String vehicleType = "car";

  private Boolean isConvertible = true;

  public String getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(String vehicleType) {
    this.vehicleType = vehicleType;
  }

  public Boolean getConvertible() {
    return isConvertible;
  }

  public void setConvertible(Boolean convertible) {
    isConvertible = convertible;
  }

}
