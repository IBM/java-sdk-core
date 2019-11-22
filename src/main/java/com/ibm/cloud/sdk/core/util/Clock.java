package com.ibm.cloud.sdk.core.util;

/**
 * Wrapper class for getting the current time. This class is especially useful for mocking the current time in tests.
 */
public class Clock {

  private Clock() { }

  public static long getCurrentTimeInMillis() {
    return System.currentTimeMillis();
  }

  public static long getCurrentTimeInSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
