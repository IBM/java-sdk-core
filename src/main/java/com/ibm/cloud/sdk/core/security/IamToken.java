/**
 * (C) Copyright IBM Corp. 2015, 2019.
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

import com.google.gson.annotations.SerializedName;
import com.ibm.cloud.sdk.core.service.model.ObjectModel;

/**
 * Represents response from IAM API.
 */
public class IamToken extends AbstractToken implements ObjectModel, TokenServerResponse {
  @SerializedName("access_token")
  private String accessToken;
  @SerializedName("refresh_token")
  private String refreshToken;
  @SerializedName("token_type")
  private String tokenType;
  @SerializedName("expires_in")
  private Long expiresIn;
  private Long expiration;

  @Override
  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public Long getExpiresIn() {
    return expiresIn;
  }

  public Long getExpiration() {
    return expiration;
  }

  /**
   * Check if currently stored access token is valid.
   *
   * Using a buffer to prevent the edge case of the
   * token expiring before the request could be made.
   *
   * The buffer will be a fraction of the total TTL. Using 80%.
   *
   * @return true iff the current access token is valid and not expired
   */
  @Override
  public boolean isTokenValid() {
    if (getAccessToken() == null || getExpiresIn() == null || getExpiration() == null) {
      return false;
    }

    Double fractionOfTimeToLive = 0.8;
    Long timeToLive = getExpiresIn();
    Long expirationTime = getExpiration();
    Double refreshTime = expirationTime - (timeToLive * (1.0 - fractionOfTimeToLive));
    Double currentTime = Math.floor(System.currentTimeMillis() / 1000);

    return currentTime < refreshTime;
  }

  /**
   * Check if the currently stored access token is expired. This is different from the isTokenValid method in that it
   * uses the actual TTL to calculate the expiration, rather than just a fraction.
   *
   * @return true iff is the current access token is not expired
   */
  @Override
  public boolean isTokenExpired() {
    return Math.floor(System.currentTimeMillis() / 1000) >= this.expiration;
  }

  /**
   * Advances the refresh time of the currently stored access token by 60 seconds.
   */
  @Override
  public void advanceRefreshTime() {
    this.expiration += 60;
  }
}
