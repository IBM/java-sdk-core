/**
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

package com.ibm.cloud.sdk.core.security;

import okhttp3.Request.Builder;

/**
 * This interface defines the common methods and constants associated with an Authenticator implementation.
 */
public interface Authenticator {

  /**
   * These are the valid authentication types.
   */
  String AUTHTYPE_BASIC = "basic";
  String AUTHTYPE_NOAUTH = "noAuth";
  String AUTHTYPE_IAM = "iam";
  String AUTHTYPE_CP4D = "cp4d";
  String AUTHTYPE_BEARER_TOKEN = "bearerToken";

  /**
   * Constants which define the names of external config propreties (credential file, environment variable, etc.).
   */
  String PROPNAME_AUTH_TYPE = "AUTH_TYPE";
  String PROPNAME_USERNAME = "USERNAME";
  String PROPNAME_PASSWORD = "PASSWORD";
  String PROPNAME_BEARER_TOKEN = "BEARER_TOKEN";
  String PROPNAME_URL = "AUTH_URL";
  String PROPNAME_DISABLE_SSL = "AUTH_DISABLE_SSL";
  String PROPNAME_APIKEY = "APIKEY";
  String PROPNAME_CLIENT_ID = "CLIENT_ID";
  String PROPNAME_CLIENT_SECRET = "CLIENT_SECRET";

  /**
   * Validates the current set of configuration information in the Authenticator.
   */
  void validate();

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
