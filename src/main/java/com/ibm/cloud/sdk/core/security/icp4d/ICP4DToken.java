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
package com.ibm.cloud.sdk.core.security.icp4d;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.security.jwt.JsonWebToken;

/**
 * This class holds relevant info re: an ICP4D access token for use by the ICP4DAuthenticator class.
 */
public class ICP4DToken {
  public String accessToken;
  public long expirationTimeInMillis;

  /**
   * This ctor is used to store a user-managed access token which will never expire.
   * @param accessToken the user-managed access token
   */
  public ICP4DToken(String accessToken) {
    this.accessToken = accessToken;
    this.expirationTimeInMillis = -1;
  }

  /**
   * This ctor will extract the ICP4D access token from the specified ICP4DTokenResponse instance,
   * and compute the expiration time as "80% of the timeToLive added to the issued-at time".
   * This means that we'll trigger the acquisition of a new token shortly before it is set to expire.
   * @param response the ICP4DTokenResponse instance
   */
  public ICP4DToken(ICP4DTokenResponse response) {
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
  public boolean isTokenValid() {
    return StringUtils.isNotEmpty(this.accessToken)
        && (this.expirationTimeInMillis < 0 || System.currentTimeMillis() <= this.expirationTimeInMillis);
  }
}
