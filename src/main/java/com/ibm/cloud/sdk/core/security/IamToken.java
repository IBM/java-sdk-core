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
   * Returns true iff currently stored access token is invalid.
   *
   * This method uses a buffer to prevent the edge case of the
   * token expiring before the request could be made, so this method will return true
   * if the current time is within that buffer.
   *
   * The buffer will be a fraction of the total TTL. Using 80%.
   *
   * This method also updates the expiration time if it determines the token needs refreshed to prevent other
   * threads from making multiple refresh calls.
   *
   * @return true iff the current access token is invalid or past the expiration buffer
   */
  @Override
  public synchronized boolean needsRefresh() {
    if (getAccessToken() == null || getExpiresIn() == null || getExpiration() == null) {
      return true;
    }

    Double fractionOfTimeToLive = 0.8;
    Long timeToLive = getExpiresIn();
    Long expirationTime = getExpiration();
    Double refreshTime = expirationTime - (timeToLive * (1.0 - fractionOfTimeToLive));
    Double currentTime = Math.floor(System.currentTimeMillis() / 1000);

    if (currentTime > refreshTime) {
      // Advance expiration time by one minute.
      this.expiration += 60;

      return true;
    }

    return false;
  }

  /**
   * Check if the currently stored access token is valid. This is different from the needsRefresh method in that it
   * uses the actual TTL to calculate the expiration, rather than just a fraction.
   *
   * @return true iff is the current access token is not expired
   */
  @Override
  public boolean isTokenValid() {
    return Math.floor(System.currentTimeMillis() / 1000) < this.expiration;
  }
}
