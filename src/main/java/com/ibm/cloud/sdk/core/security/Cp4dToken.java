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
package com.ibm.cloud.sdk.core.security;

import org.apache.commons.lang3.StringUtils;

/**
 * This class holds relevant info re: a CP4D access token for use by the CloudPakForDataAuthenticator class.
 */
public class Cp4dToken extends AbstractToken {
  public String accessToken;
  public long expirationTimeInMillis;

  /**
   * This ctor is used to store a user-managed access token which will never expire.
   * @param accessToken the user-managed access token
   */
  public Cp4dToken(String accessToken) {
    this.accessToken = accessToken;
    this.expirationTimeInMillis = -1;
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
    if (iat != null && exp != null) {
      long ttl = exp.longValue() - iat.longValue();
      this.expirationTimeInMillis = (iat.longValue() + (long) (0.8 * ttl)) * 1000;
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
        && (this.expirationTimeInMillis < 0 || System.currentTimeMillis() <= this.expirationTimeInMillis);
  }

  /**
   * @return the access token value from this
   */
  @Override
  public String getAccessToken() {
    return this.accessToken;
  }
}
