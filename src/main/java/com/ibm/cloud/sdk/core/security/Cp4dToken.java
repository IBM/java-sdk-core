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

import org.apache.commons.lang3.StringUtils;

/**
 * This class holds relevant info re: a CP4D access token for use by the CloudPakForDataAuthenticator class.
 */
public class Cp4dToken extends AbstractToken {
  private String accessToken;
  private long expirationTimeInMillis;
  private long refreshTimeInMillis;

  /**
   * This ctor is used to store a user-managed access token which will never expire.
   * @param accessToken the user-managed access token
   */
  public Cp4dToken(String accessToken) {
    this.accessToken = accessToken;
    this.refreshTimeInMillis = -1;
  }

  /**
   * This ctor will extract the ICP4D access token from the specified Cp4dTokenResponse instance,
   * and compute the expiration time as "80% of the timeToLive added to the issued-at time".
   * This means that we'll trigger the acquisition of a new token shortly before it is set to expire.
   * @param response the Cp4dTokenResponse instance
   */
  public Cp4dToken(Cp4dTokenResponse response) {
    this.accessToken = response.getAccessToken();

    // To compute the expiration time, we'll need to crack open the accessToken value
    // which is a JWToken (Json Web Token) instance.
    JsonWebToken jwt = new JsonWebToken(this.accessToken);

    Long iat = jwt.getPayload().getIssuedAt();
    Long exp = jwt.getPayload().getExpiresAt();
    this.expirationTimeInMillis = jwt.getPayload().getExpiresAt();

    if (iat != null && exp != null) {
      long ttl = exp.longValue() - iat.longValue();
      this.refreshTimeInMillis = (iat.longValue() + (long) (0.8 * ttl)) * 1000;
    } else {
      throw new RuntimeException("Properties 'iat' and 'exp' MUST be present within the encoded access token");
    }
  }

  /**
   * Returns true iff this object holds a valid non-expired access token.
   * @return true if token is valid and not expired, false otherwise
   */
  @Override
  public boolean isTokenValid() {
    return StringUtils.isNotEmpty(this.accessToken)
        && (this.refreshTimeInMillis < 0 || System.currentTimeMillis() <= this.refreshTimeInMillis);
  }

  /**
   * Check if the currently stored access token is expired. This is different from the isTokenValid method in that it
   * uses the actual TTL to calculate the expiration, rather than just a fraction.
   *
   * @return true iff is the current access token is not expired
   */
  @Override
  public boolean isTokenExpired() {
    return System.currentTimeMillis() >= this.expirationTimeInMillis;
  }

  /**
   * Advances the refresh time of the currently stored access token by 60 seconds.
   */
  @Override
  public void advanceRefreshTime() {
    this.refreshTimeInMillis += 60000;
  }

  /**
   * @return the access token value from this
   */
  @Override
  public String getAccessToken() {
    return this.accessToken;
  }
}
