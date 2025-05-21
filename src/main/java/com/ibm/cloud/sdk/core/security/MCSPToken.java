/**
 * (C) Copyright IBM Corp. 2023.
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

import com.ibm.cloud.sdk.core.util.Clock;
import org.apache.commons.lang3.StringUtils;

/**
 * This class holds the cached information related to an access token
 * received by the Multi-Cloud Saas Platform (MCSP) Authenticator.
 */
public class MCSPToken extends AbstractToken {
  private String accessToken;
  private long expirationTime;
  private long refreshTime;

  public MCSPToken() {
    super();
  }

  /**
   * This ctor is used to store a user-managed access token which will never expire.
   * @param accessToken the user-managed access token
   */
  public MCSPToken(String accessToken) {
    super();
    this.accessToken = accessToken;
    this.expirationTime = -1;
    this.refreshTime = -1;
  }

  /**
   * This ctor is used to store an exception which indicates an error with the most recent
   * token server interaction.
   * @param t the exception to store in this object
   */
  public MCSPToken(Throwable t) {
    super(t);
  }

  /**
   * This ctor will extract the access token from the specified MCSPTokenResponse instance,
   * and compute the refresh time as "80% of the timeToLive added to the issued-at time".
   * This means that we'll trigger the acquisition of a new token shortly before it is set to expire.
   * @param response the MCSPTokenResponse instance
   */
  public MCSPToken(MCSPTokenResponse response) {
    super();
    this.accessToken = response.getToken();

    // To compute the expiration time, we'll need to crack open the accessToken value
    // which is a JWT (Json Web Token) instance.
    JsonWebToken jwt = new JsonWebToken(this.accessToken);

    Long iat = jwt.getPayload().getIssuedAt();
    Long exp = jwt.getPayload().getExpiresAt();

    if (iat != null && exp != null) {
      long ttl = exp - iat;

      this.expirationTime = exp;
      this.refreshTime = iat + (long) (0.8 * ttl);
    } else {
      throw new RuntimeException("Properties 'iat' and 'exp' MUST be present within the encoded access token");
    }
  }

  /**
   * This ctor will extract the access token from the specified MCSPV2TokenResponse instance,
   * and compute the refresh time as "80% of the timeToLive added to the issued-at time".
   * This means that we'll trigger the acquisition of a new token shortly before it is set to expire.
   * @param response the MCSPTokenResponse instance
   */
  public MCSPToken(MCSPV2TokenResponse response) {
    super();
    this.accessToken = response.getToken();

    // To compute the expiration time, we'll need to crack open the accessToken value
    // which is a JWT (Json Web Token) instance.
    JsonWebToken jwt = new JsonWebToken(this.accessToken);

    Long iat = jwt.getPayload().getIssuedAt();
    Long exp = jwt.getPayload().getExpiresAt();

    if (iat != null && exp != null) {
      long ttl = exp - iat;

      this.expirationTime = exp;
      this.refreshTime = iat + (long) (0.8 * ttl);
    } else {
      throw new RuntimeException("Properties 'iat' and 'exp' MUST be present within the encoded access token");
    }
  }

  /**
   * Returns true iff this object does not hold a valid access token or has one which has crossed our refresh
   * time. This method also updates the refresh time if it determines the token needs to be refreshed to
   * prevent other threads from making multiple refresh calls.
   *
   * @return true if token is invalid or past the refresh time, false otherwise
   */
  @Override
  public synchronized boolean needsRefresh() {
    if (this.getException() != null) {
      return true;
    }

    if (StringUtils.isEmpty(this.accessToken)
        || (this.refreshTime >= 0 && Clock.getCurrentTimeInSeconds() > this.refreshTime)) {
      // Advance refresh time by one minute.
      this.refreshTime = Clock.getCurrentTimeInSeconds() + 60;

      return true;
    }

    return false;
  }

  /**
   * Check if the currently stored access token is valid. This is different from the needsRefresh
   * method in that it uses the actual TTL to calculate the expiration, rather than just a fraction.
   *
   * @return true iff the current access token is not expired
   */
  @Override
  public boolean isTokenValid() {
    return (this.getException() == null)
        && (this.expirationTime >= 0) && (Clock.getCurrentTimeInSeconds() < this.expirationTime);
  }

  /**
   * @return the access token value from this
   */
  @Override
  public String getAccessToken() {
    return this.accessToken;
  }
}
