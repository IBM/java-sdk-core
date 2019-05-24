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
package com.ibm.cloud.sdk.security.jwt;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

import java.util.Base64.Decoder;

/**
 * This class is used to decode and parse a JWT (Json Web Token).
 */
public class JsonWebToken {
  private static Decoder decoder = Base64.getUrlDecoder();

  private Map<String, String> header;
  private Map<String, Object> payload;

  public JsonWebToken(String encodedToken) {
    // Split the encoded jwt string into the header, payload, and signature
    String[] decodedParts = encodedToken.split("\\.");

    String json;
    Type headerType = new TypeToken<Map<String, String>>(){}.getType();
    Type payloadType = new TypeToken<Map<String, Object>>(){}.getType();

    // Decode and parse the header.
    json = new String(decoder.decode(decodedParts[0]));
    header = GsonSingleton.getGson().fromJson(json, headerType);

    // Decode and parse the body.
    json = new String(decoder.decode(decodedParts[1]));
    payload = GsonSingleton.getGson().fromJson(json, payloadType);
  }

  public Map<String, String> getHeader() {
    return header;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  /**
   * Returns the "Issued At" ("iat") value within this JsonWebToken.
   */
  public Long getIssuedAt() {
    return getPayloadValueAsLong("iat");
  }

  /**
   * Returns the "Expires AT" ("exp") value within this JsonWebToken.
   */
  public Long getExpiresAt() {
    return getPayloadValueAsLong("exp");
  }

  /**
   * Returns the "Subject" ("sub") value with this JsonWebToken.
   */
  public String getSubject() {
    return getPayloadValueAsString("sub");
  }

  /**
   * Returns the "Issuer" ("iss") value with this JsonWebToken.
   */
  public String getIssuer() {
    return getPayloadValueAsString("iss");
  }

  /**
   * Returns the "Audience" ("aud") value with this JsonWebToken.
   */
  public String getAudience() {
    return getPayloadValueAsString("aud");
  }

  /**
   * Returns the "Userid" ("uid") value with this JsonWebToken.
   */
  public String getUserid() {
    return getPayloadValueAsString("uid");
  }

  /**
   * Returns the "Username" ("username") value with this JsonWebToken.
   */
  public String getUsername() {
    return getPayloadValueAsString("username");
  }

  /**
   * Returns the "Role" ("role") value with this JsonWebToken.
   */
  public String getRole() {
    return getPayloadValueAsString("role");
  }

  /**
   * Retrieves the specified property from this JsonWebToken's payload and returns it as a {@link Long} value.
   * @param propertyName the name of the property to retrieve
   * @return the value of the specified property as a {@link Long} value
   */
  protected Long getPayloadValueAsLong(String propertyName) {
    Long result = null;
    Object o = payload.get(propertyName);
    if (o != null) {
      if (o instanceof String) {
        result = Long.valueOf((String) o);
      } else if (o instanceof Integer) {
        result = Long.valueOf(((Integer) o).intValue());
      } else if (o instanceof Long) {
        result = (Long) o;
      } else {
        throw new RuntimeException("Unexpected value for JWT payload property `"
            + propertyName + "': " + o.toString());
      }
    }

    return result;
  }

  /**
   * Retrieves the specified property from this JsonWebToken's payload and returns it as a {@link String} value.
   * @param propertyName the name of the property to retrieve
   * @return the value of the specified property as a {@link String} value
   */
  protected String getPayloadValueAsString(String propertyName) {
    String result = null;
    Object o = payload.get(propertyName);
    if (o != null) {
      result = o.toString();
    }

    return result;
  }
}

