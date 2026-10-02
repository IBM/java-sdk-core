/**
 * (C) Copyright IBM Corp. 2019, 2023.
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

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
    json = new String(Base64.getUrlDecoder().decode(decodedParts[0]), Charset.forName("UTF-8"));
    header = GsonSingleton.getGson().fromJson(json, headerType);

    // Decode and parse the body.
    json = new String(Base64.getUrlDecoder().decode(decodedParts[1]), Charset.forName("UTF-8"));
    payload = GsonSingleton.getGson().fromJson(json, Payload.class);
  }

  public Map<String, String> getHeader() {
    return header;
  }

  public Payload getPayload() {
    return payload;
  }

  /**
   * Gson TypeAdapter that reads the "aud" claim as either a plain string or a JSON array of strings,
   * per RFC 7519 §4.1.3.
   */
  static class AudienceTypeAdapter extends TypeAdapter<List<String>> {
    @Override
    public List<String> read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      List<String> result = new ArrayList<>();
      if (in.peek() == JsonToken.BEGIN_ARRAY) {
        in.beginArray();
        while (in.hasNext()) {
          result.add(in.nextString());
        }
        in.endArray();
      } else {
        result.add(in.nextString());
      }
      return result;
    }

    @Override
    public void write(JsonWriter out, List<String> value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginArray();
      for (String s : value) {
        out.value(s);
      }
      out.endArray();
    }
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
    @JsonAdapter(AudienceTypeAdapter.class)
    private List<String> audience;
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
     * Returns the first "Audience" ("aud") value within this JsonWebToken.
     * Per RFC 7519 §4.1.3, the aud claim may be a string or an array of strings.
     * This method returns the first element for backward compatibility.
     * Use {@link #getAudiences()} to retrieve all values.
     * @return the first aud value, or null if absent
     */
    public String getAudience() {
      return (audience != null && !audience.isEmpty()) ? audience.get(0) : null;
    }

    /**
     * Returns all "Audience" ("aud") values within this JsonWebToken.
     * Per RFC 7519 §4.1.3, the aud claim may be a string or an array of strings;
     * this method always returns a list regardless of the original JSON form.
     * @return a list of aud values, or null if absent
     */
    public List<String> getAudiences() {
      return audience != null ? Collections.unmodifiableList(audience) : null;
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

