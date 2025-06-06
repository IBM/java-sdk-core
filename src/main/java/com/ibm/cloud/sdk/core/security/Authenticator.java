/**
 * (C) Copyright IBM Corp. 2019, 2024.
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
  String AUTHTYPE_IAM_ASSUME = "iamAssume";
  String AUTHTYPE_CP4D = "cp4d";
  String AUTHTYPE_CP4D_SERVICE = "cp4dService";
  String AUTHTYPE_CP4D_SERVICE_INSTANCE = "cp4dServiceInstance";
  String AUTHTYPE_BEARER_TOKEN = "bearerToken";
  String AUTHTYPE_CONTAINER = "container";
  String AUTHTYPE_VPC = "vpc";
  String AUTHTYPE_MCSP = "mcsp";
  String AUTHTYPE_MCSPV2 = "mcspv2";

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
  String PROPNAME_SERVICE_INSTANCE_ID = "SERVICE_INSTANCE_ID";
  String PROPNAME_CLIENT_ID = "CLIENT_ID";
  String PROPNAME_CLIENT_SECRET = "CLIENT_SECRET";
  String PROPNAME_SCOPE = "SCOPE";
  String PROPNAME_UID = "UID";
  String PROPNAME_DISPLAY_NAME = "DISPLAY_NAME";
  String PROPNAME_PERMISSIONS = "PERMISSIONS";
  String PROPNAME_EXPIRATION_TIME = "EXPIRATION_TIME";
  String PROPNAME_SERVICE_BROKER_SECRET = "SERVICE_BROKER_SECRET";
  String PROPNAME_CR_TOKEN_FILENAME = "CR_TOKEN_FILENAME";
  String PROPNAME_IAM_PROFILE_CRN = "IAM_PROFILE_CRN";
  String PROPNAME_IAM_PROFILE_ID = "IAM_PROFILE_ID";
  String PROPNAME_IAM_PROFILE_NAME = "IAM_PROFILE_NAME";
  String PROPNAME_IAM_ACCOUNT_ID = "IAM_ACCOUNT_ID";
  String PROPNAME_SCOPE_COLLECTION_TYPE = "SCOPE_COLLECTION_TYPE";
  String PROPNAME_SCOPE_ID = "SCOPE_ID";
  String PROPNAME_INCLUDE_BUILTIN_ACTIONS = "INCLUDE_BUILTIN_ACTIONS";
  String PROPNAME_INCLUDE_CUSTOM_ACTIONS = "INCLUDE_CUSTOM_ACTIONS";
  String PROPNAME_INCLUDE_ROLES = "INCLUDE_ROLES";
  String PROPNAME_PREFIX_ROLES = "PREFIX_ROLES";
  String PROPNAME_CALLER_EXT_CLAIM = "CALLER_EXT_CLAIM";

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
