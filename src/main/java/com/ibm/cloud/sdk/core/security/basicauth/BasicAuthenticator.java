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
package com.ibm.cloud.sdk.core.security.basicauth;

import java.util.Base64;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;

import okhttp3.Request.Builder;

/**
 * This class implements support for Basic Authentication. The main purpose of this authenticator is to construct the
 * Authorization header and then add it to each outgoing REST API request.
 */
public class BasicAuthenticator implements Authenticator {

  // The cached value of the Authorization header.
  private String authHeader;

  // Hide the default ctor to force use of the 1-arg ctor below.
  @SuppressWarnings("unused")
  private BasicAuthenticator() {
  }

  /**
   * Initialize our Authorization header value using the information in the BasicAuthConfig instance.
   * This ctor assumes that the config object has already passed validation.
   *
   * @param config
   *          the BasicAuthConfig instance that holds the username and password values from which to build the
   *          Authorization header.
   */
  public BasicAuthenticator(BasicAuthConfig config) {
    this.authHeader =
        "Basic " + Base64.getEncoder().encodeToString((config.getUsername() + ":" + config.getPassword()).getBytes());
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_BASIC;
  }

  /**
   * This method is called to authenticate an outgoing REST API request.
   * Here, we'll just set the Authorization header to provide the necessary authentication info.
   */
  @Override
  public void authenticate(Builder builder) {
    builder.addHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
  }
}
