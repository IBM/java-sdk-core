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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

import okhttp3.Request.Builder;

/**
 * This class implements support for Basic Authentication.
 * The main purpose of this authenticator is to construct the Authorization header,
 * and then add it to each outgoing REST API request.
 */
public class BasicAuthenticator extends AuthenticatorBase implements Authenticator {
  private String username;
  private String password;

  // The cached value of the Authorization header.
  private String authHeader;

  // The default ctor is hidden to force the use of the non-default ctors.
  protected BasicAuthenticator() {
  }

  /**
   * Construct a BasicAuthenticator instance with the specified username and password.
   * These values are used to construct an Authorization header value that will be included
   * in outgoing REST API requests.
   *
   * @param username the basic auth username
   * @param password the basic auth password
   */
  public BasicAuthenticator(String username, String password) {
    init(username, password);
  }

  /**
   * Construct a BasicAuthenticator using properties retrieved from the specified Map.
   * @param config a map containing the username and password values
   */
  public BasicAuthenticator(Map<String, String> config) {
    init(config.get(PROPNAME_USERNAME), config.get(PROPNAME_PASSWORD));
  }

  private void init(String username, String password) {
    this.username = username;
    this.password = password;
    this.validate();

    // Cache the Authorization header value.
    this.authHeader = constructBasicAuthHeader(this.username, this.password);
  }

  @Override
  public void validate() {
    if (StringUtils.isEmpty(username)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "username"));
    }

    if (StringUtils.isEmpty(password)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "password"));
    }

    if (CredentialUtils.hasBadStartOrEndChar(username)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_INVALID, "username"));
    }

    if (CredentialUtils.hasBadStartOrEndChar(password)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_INVALID, "password"));
    }
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_BASIC;
  }

  /**
   * @return the username configured on this Authenticator
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @return the password configured on this Authenticator
   */
  public String getPassword() {
    return this.password;
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
