/**
 * (C) Copyright IBM Corp. 2024.
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

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.RequestBuilder;

/**
 * This class contains code that is common to all authenticators that need to
 * interact with the IAM token service to obtain an access token.
 */
public abstract class IamRequestBasedAuthenticatorImmutable
  extends TokenRequestBasedAuthenticatorImmutable<IamToken, IamToken>
  implements Authenticator {

  protected static final String DEFAULT_IAM_URL = "https://iam.cloud.ibm.com";

  // Properties common to IAM-based authenticators.
  protected String url;
  protected String scope;
  protected String clientId;
  protected String clientSecret;

  // This is the value of the Authorization header we'll use when interacting with the token server.
  protected String cachedAuthorizationHeader = null;


  @Override
  public void validate() {
    if (StringUtils.isEmpty(getClientId()) && StringUtils.isEmpty(getClientSecret())) {
      // both empty is ok.
    } else {
      if (StringUtils.isEmpty(getClientId())) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "clientId"));
      }
      if (StringUtils.isEmpty(getClientSecret())) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "clientSecret"));
      }
    }

    // Assuming everything validates clean, let's cache the basic auth header if
    // the clientId/clientSecret properties are configured.
    this.cachedAuthorizationHeader = constructBasicAuthHeader(this.clientId, this.clientSecret);
  }

  /**
   * @return the URL configured on this Authenticator.
   */
  public String getURL() {
    return this.url;
  }

  /**
   * @return the clientId configured on this Authenticator.
   */
  public String getClientId() {
    return this.clientId;
  }

  /**
   * @return the clientSecret configured on this Authenticator.
   */
  public String getClientSecret() {
    return this.clientSecret;
  }

  /**
   * @return the scope parameter
   */
  public String getScope() {
    return this.scope;
  }

  /**
   * Sets the URL on this Authenticator.
   * @param url the URL representing the IAM token server endpoint
   */
  protected void _setURL(String url) {
    if (StringUtils.isEmpty(url)) {
      url = DEFAULT_IAM_URL;
    }
    this.url = url;
  }


  /**
   * If a basic auth Authorization header is cached in "this", then add it to
   * the specified request builder.
   * This is used in situations where we want to add an Authorization header
   * containing basic auth information to the token exchange request itself.
   * @param builder the request builder
   */
  protected void addAuthorizationHeader(RequestBuilder builder) {
    if (StringUtils.isNotEmpty(this.cachedAuthorizationHeader)) {
      builder.header(HttpHeaders.AUTHORIZATION, this.cachedAuthorizationHeader);
    }
  }
}
