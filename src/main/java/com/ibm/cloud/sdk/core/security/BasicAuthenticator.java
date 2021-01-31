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


  /**
   * This Builder class is used to construct BasicAuthenticator instances.
   */
  public static class Builder {
    private String username;
    private String password;

    // Default ctor.
    public Builder() { }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(BasicAuthenticator obj) {
      this.username = obj.username;
      this.password = obj.password;
    }

    /**
     * Constructs a new instance of BasicAuthenticator from the builder's configuration.
     *
     * @return the BasicAuthenticator instance
     */
    public BasicAuthenticator build() {
      return new BasicAuthenticator(this);
    }

    /**
     * Sets the username property.
     * @param username the base auth username to include in the Authorization header
     * @return the Builder
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Sets the password property.
     * @param password the basic auth password to include in the Authorization header
     * @return the Builder
     */
    public Builder password(String password) {
      this.password = password;
      return this;
    }
  }

  // The default ctor is hidden to force the use of the non-default ctors.
  protected BasicAuthenticator() {
  }

  /**
   * Constructs a BasicAuthenticator instance from the configuration
   * contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected BasicAuthenticator(Builder builder) {
    this.username = builder.username;
    this.password = builder.password;
    this.validate();
  }

  /**
   * Returns a new Builder instance pre-loaded with the configuration from "this".
   *
   * @return the builder
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Construct a BasicAuthenticator instance with the specified username and password.
   * These values are used to construct an Authorization header value that will be included
   * in outgoing REST API requests.
   *
   * @param username the basic auth username
   * @param password the basic auth password
   *
   * @deprecated As of 9.7.0, use the Builder class instead.
   *
   */
  @Deprecated
  public BasicAuthenticator(String username, String password) {
    init(username, password);
  }

  /**
   * Construct a BasicAuthenticator using properties retrieved from the specified Map.
   *
   * @param config a map containing the username and password values
   *
   * @deprecated As of 9.7.0, use BasicAuthenticator.fromConfiguration() instead.
   */
  @Deprecated
  public BasicAuthenticator(Map<String, String> config) {
    init(config.get(PROPNAME_USERNAME), config.get(PROPNAME_PASSWORD));
  }

  /**
   * Construct a BasicAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the BasicAuthenticator instance
   *
   */
  public static BasicAuthenticator fromConfiguration(Map<String, String> config) {
    return new Builder()
      .username(config.get(PROPNAME_USERNAME))
      .password(config.get(PROPNAME_PASSWORD))
      .build();
  }

  private void init(String username, String password) {
    this.username = username;
    this.password = password;
    this.validate();
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

    // Cache the Authorization header value.
    this.authHeader = constructBasicAuthHeader(this.username, this.password);
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
  public void authenticate(okhttp3.Request.Builder builder) {
    builder.addHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
  }
}
