/**
 * Copyright 2019 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.security;

import okhttp3.Request.Builder;

/**
 * This interface defines the common methods associated with an Authenticator implementation.
 */
public interface Authenticator {

  /**
   * These are the valid authentication types.
   */
  String AUTHTYPE_BASIC = "basic";
  String AUTHTYPE_NOAUTH = "noauth";
  String AUTHTYPE_IAM = "iam";
  String AUTHTYPE_ICP4D = "icp4d";

  /**
   * Returns the authentication type associated with the Authenticator instance.
   *
   * @return a string representing the authentication type (e.g. "iam", "basic", "icp4d", etc.)
   */
  String authenticationType();

  /**
   * Perform the necessary authentication steps for the specified request.
   *
   * @param requestBuilder
   *          the {@link okhttp3.Request.Builder} instance to authenticate
   */
  void authenticate(Builder requestBuilder);
}
