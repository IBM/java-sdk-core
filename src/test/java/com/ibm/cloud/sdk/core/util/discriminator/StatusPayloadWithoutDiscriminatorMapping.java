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

public class StatusPayloadWithoutDiscriminatorMapping extends GenericModel {

  protected static String discriminatorPropertyName = "action";

  // the below part is commented out and left here demonstrating that we are testing a lack of the code below

  // protected static java.util.Map<String, Class<?>> discriminatorMapping;
  // static {
  //   discriminatorMapping = new java.util.HashMap<>();
  //   discriminatorMapping.put("accept", AcceptPayloadWithoutDiscriminatorPropertyName.class);
  // }

  protected String action;

  protected StatusPayloadWithoutDiscriminatorMapping() {
  }

  public String action() {
    return action;
  }

}
