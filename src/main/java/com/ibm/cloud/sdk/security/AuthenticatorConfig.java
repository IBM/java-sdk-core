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
package com.ibm.cloud.sdk.security;

/**
 * This interface defines the common methods associated with an "authenticator configuration".
 * Each authenticator implementation will have a corresponding "config" class which implements this
 * interface and models the configuration information associated with that authenticator.
 */
public interface AuthenticatorConfig {

  /**
   * Returns the authentication type associated with the AuthenticatorConfig instance.
   * @return a string representing the authentication type (e.g. "iam", "basic", "icp4d", "noauth", etc.)
   */
  String authenticationType();

  /**
   * Validate the configuration and throw an exception if validation fails.
   * @throws IllegalArgumentException if validation failed
   */
  void validate() throws IllegalArgumentException;

}
