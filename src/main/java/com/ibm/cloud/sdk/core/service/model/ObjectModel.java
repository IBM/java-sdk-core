package com.ibm.cloud.sdk.core.service.model;

/**
 * Interface for both generic and dynamic model classes.
 */
public interface ObjectModel {
  @Override
  boolean equals(Object o);

  @Override
  int hashCode();

  @Override
  String toString();
}
