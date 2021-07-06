/*
 * (C) Copyright IBM Corp. 2021.
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
package com.ibm.cloud.sdk.core.util.discriminator;

import com.ibm.cloud.sdk.core.service.model.GenericModel;

public class StatusPayload extends GenericModel {
  @SuppressWarnings("unused")
  protected static String discriminatorPropertyName = "action";
  protected static java.util.Map<String, Class<?>> discriminatorMapping;
  static {
    discriminatorMapping = new java.util.HashMap<>();
    discriminatorMapping.put("resolve", ResolvePayload.class);
  }

  protected String action;

  protected StatusPayload() {
  }

  public String action() {
    return action;
  }

}

