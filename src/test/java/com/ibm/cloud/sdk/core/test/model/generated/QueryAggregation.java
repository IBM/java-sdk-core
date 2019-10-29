/*
 * (C) Copyright IBM Corp. 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.cloud.sdk.core.test.model.generated;

import com.ibm.cloud.sdk.core.service.model.GenericModel;

/**
 * Common base type for query aggregations.
 *
 * Classes which extend this class:
 * - QueryTermAggregation
 * - QueryNestedAggregation
 */
public class QueryAggregation extends GenericModel {
  @SuppressWarnings("unused")
  protected static String discriminatorPropertyName = "type";
  protected static java.util.Map<String, Class<?>> discriminatorMapping;
  static {
    discriminatorMapping = new java.util.HashMap<>();
    discriminatorMapping.put("term", QueryTermAggregation.class);
    discriminatorMapping.put("nested", QueryNestedAggregation.class);
  }

  protected String type;
  protected String field;
  protected Long count;
  protected String path;

  protected QueryAggregation() {
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the field.
   *
   * The field where the aggregation is located in the document.
   *
   * @return the field
   */
  public String getField() {
    return field;
  }

  /**
   * Gets the count.
   *
   * Aggregation count.
   *
   * @return the count
   */
  public Long getCount() {
    return count;
  }

  /**
   * Gets the path.
   *
   * The path to the nested document field to perform additional aggregations.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }
}

