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

import com.google.common.io.BaseEncoding;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * This class is used to decode and parse a JWT (Json Web Token).
 */
public class JsonWebToken {
  private Map<String, String> header;
  private Payload payload;

  /**
   * Ctor which accepts the encoded JWT as a string.  This ctor will parse
   * the JWT into its header and payload parts
   * @param encodedToken a string representing the encoded JWT.
   */
  public JsonWebToken(String encodedToken) {
    // Split the encoded jwt string into the header, payload, and signature
    String[] decodedParts = encodedToken.split("\\.");

    String json;
    Type headerType = new TypeToken<Map<String, String>>(){}.getType();

    // Decode and parse the header.
    json = new String(BaseEncoding.base64Url().decode(decodedParts[0]));
    header = GsonSingleton.getGson().fromJson(json, headerType);

    // Decode and parse the body.
    json = new String(BaseEncoding.base64Url().decode(decodedParts[1]));
    payload = GsonSingleton.getGson().fromJson(json, Payload.class);
  }

  public Map<String, String> getHeader() {
    return header;
  }

  public Payload getPayload() {
    return payload;
  }

  public class Payload {
    @SerializedName("iat")
    private Long issuedAt;
    @SerializedName("exp")
    private Long expiresAt;
    @SerializedName("sub")
    private String subject;
    @SerializedName("iss")
    private String issuer;
    @SerializedName("aud")
    private String audience;
    @SerializedName("uid")
    private String userId;
    private String username;
    private String role;

    public Payload() {}

    /**
     * Returns the "Issued At" ("iat") value within this JsonWebToken.
     * @return the iat value
     */
    public Long getIssuedAt() {
      return issuedAt;
    }

    /**
     * Returns the "Expires At" ("exp") value within this JsonWebToken.
     * @return the exp value
     */
    public Long getExpiresAt() {
      return expiresAt;
    }

    /**
     * Returns the "Subject" ("sub") value with this JsonWebToken.
     * @return the sub value
     */
    public String getSubject() {
      return subject;
    }

    /**
     * Returns the "Issuer" ("iss") value with this JsonWebToken.
     * @return the iss value
     */
    public String getIssuer() {
      return issuer;
    }

    /**
     * Returns the "Audience" ("aud") value with this JsonWebToken.
     * @return the aud value
     */
    public String getAudience() {
      return audience;
    }

    /**
     * Returns the "Userid" ("uid") value with this JsonWebToken.
     * @return the uid value
     */
    public String getUserId() {
      return userId;
    }

    /**
     * Returns the "Username" ("username") value with this JsonWebToken.
     * @return the username value
     */
    public String getUsername() {
      return username;
    }

    /**
     * Returns the "Role" ("role") value with this JsonWebToken.
     * @return the role value
     */
    public String getRole() {
      return role;
    }
  }
}

