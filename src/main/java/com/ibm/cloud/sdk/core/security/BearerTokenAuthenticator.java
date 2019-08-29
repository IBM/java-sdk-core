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

import okhttp3.Request.Builder;

/**
 * This class provides an Authenticator implementation that supports a user-supplied Bear Token value.
 * The main purpose of this authenticator is to construct the Authorization header with the user-supplied
 * token, then add the header to each outgoing REST API request.
 */
public class BearerTokenAuthenticator extends AuthenticatorBase implements Authenticator {
  private String bearerToken;

  // The cached value of the Authorization header.
  private String cachedAuthHeader;

  // The default ctor is hidden to force the use of the non-default ctors.
  protected BearerTokenAuthenticator() {
  }

  /**
   * Construct a BearerTokenAuthenticator instance with the specified access token.
   * The token value will be used to construct an Authorization header that will be included
   * in outgoing REST API requests.
   *
   * @param bearerToken the access token value
   */
  public BearerTokenAuthenticator(String bearerToken) {
    setBearerToken(bearerToken);
  }

  /**
   * Construct a BearerTokenAuthenticator using properties retrieved from the specified Map.
   * @param config a map containing the access token value
   */
  public BearerTokenAuthenticator(Map<String, String> config) {
    setBearerToken(config.get(PROPNAME_BEARER_TOKEN));
  }

  @Override
  public void validate() {
    if (StringUtils.isEmpty(bearerToken)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "bearerToken"));
    }
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_BEARER_TOKEN;
  }

  /**
   * @return the access token configured for this Authenticator
   */
  public String getBearerToken() {
    return this.bearerToken;
  }

  /**
   * Sets the bearer token value to be used by this Authenticator.
   * @param bearerToken the new bearer token value
   */
  public void setBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
    this.validate();

    // Cache the Authorization header value.
    this.cachedAuthHeader = constructBearerTokenAuthHeader(this.bearerToken);
  }

  /**
   * This method is called to authenticate an outgoing REST API request.
   * Here, we'll just set the Authorization header to provide the necessary authentication info.
   */
  @Override
  public void authenticate(Builder builder) {
    builder.addHeader(HttpHeaders.AUTHORIZATION, this.cachedAuthHeader);
  }
}
