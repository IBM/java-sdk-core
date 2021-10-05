/**
 * (C) Copyright IBM Corp. 2021.
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

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * This class models the response received from the VPC "create_access_token" and
 * "create_iam_token" operations.
 */
public class VpcTokenResponse {

  @SerializedName("access_token")
  protected String accessToken;

  @SerializedName("created_at")
  protected Date createdAt;

  @SerializedName("expires_at")
  protected Date expiresAt;

  @SerializedName("expires_in")
  protected Long expiresIn;

  /**
   * Gets the accessToken.
   *
   * The access token.
   *
   * @return the accessToken
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Gets the createdAt.
   *
   * The date and time that the access token was created.
   *
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the expiresAt.
   *
   * The date and time that the access token will expire.
   *
   * @return the expiresAt
   */
  public Date getExpiresAt() {
    return expiresAt;
  }

  /**
   * Gets the expiresIn.
   *
   * Time in seconds before the access token expires.
   *
   * @return the expiresIn
   */
  public Long getExpiresIn() {
    return expiresIn;
  }
}
