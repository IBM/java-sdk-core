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

// DISCLAIMER: The JWT token strings used in this file are for testing purposes only.
// They are not valid tokens and cannot be used to authenticate with any real service.
package com.ibm.cloud.sdk.core.test.security;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.security.JsonWebToken;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class JsonWebTokenTest {

  // payload: {"username":"admin","role":"Admin","permissions":[...],"sub":"admin","iss":"KNOXSSO","aud":"DSX","uid":"999","iat":1559328955,"exp":9999999999999999999}
  private static String encodedToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFkbWluIiwicGVybWlzc2lvbnMiOlsiYWRtaW5pc3RyYXRvciIsIm1hbmFnZV9jYXRhbG9nIiwiYWNjZXNzX2NhdGFsb2ciLCJtYW5hZ2VfcG9saWNpZXMiLCJhY2Nlc3NfcG9saWNpZXMiLCJ2aXJ0dWFsaXplX3RyYW5zZm9ybSIsImNhbl9wcm92aXNpb24iLCJkZXBsb3ltZW50X2FkbWluIl0sInN1YiI6ImFkbWluIiwiaXNzIjoiS05PWFNTTyIsImF1ZCI6IkRTWCIsInVpZCI6Ijk5OSIsImlhdCI6MTU1OTMyODk1NSwiZXhwIjo5OTk5OTk5OTk5OTk5OTk5OTl9.GE-ML3JWmI3oB0z5mjMG3jFtYVVA5bQCsOTOOj9ab7PcgJc1mA5hn1sONkO0JAFADhUoAgpG4KgQef5tjnCSrtl1tbnDuhaP1DH4QKMCZOkWrKyfQ2X8P1jhyJmV-KpE4wuTrGdMoMVj4wVRZwnxMRSK6LhV6pIzyOLLYR21zcW_2KcKWxCYfIC7tiM1d2PSM5nWa_5Vb068F8PtdiFUElEYHYKrvmwpV57_k2jpXoY6zw8PDcIiWQe3g20w6vCB6zWhxbcFWyjMg1tPOZHgTNNskPShHQBbtZFsSrc7rkNPzttKF70m7_JqrRYUZDNN8TmuR9uyitwxEFkr2L0WDQ"; // #nosec

  // payload: {"username":"admin","role":"Admin","sub":"admin","iss":"KNOXSSO","aud":["account-iam.platform.saas.ibm.com"],"uid":"999","iat":1559328955,"exp":9999999999}
  private static String encodedTokenWithArrayAud = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFkbWluIiwic3ViIjoiYWRtaW4iLCJpc3MiOiJLTk9YU1NPIiwiYXVkIjpbImFjY291bnQtaWFtLnBsYXRmb3JtLnNhYXMuaWJtLmNvbSJdLCJ1aWQiOiI5OTkiLCJpYXQiOjE1NTkzMjg5NTUsImV4cCI6OTk5OTk5OTk5OX0.fakesignature"; // #nosec

  @Test
  public void testConstructor() {
    JsonWebToken jwt = new JsonWebToken(encodedToken);
    assertNotNull(jwt.getHeader());
    assertNotNull(jwt.getPayload());
    assertNotNull(jwt.getPayload().getExpiresAt());
    assertNotNull(jwt.getPayload().getIssuedAt());
    assertNotNull(jwt.getPayload().getAudience());
    assertNotNull(jwt.getPayload().getAudiences());
    assertNotNull(jwt.getPayload().getIssuer());
    assertNotNull(jwt.getPayload().getRole());
    assertNotNull(jwt.getPayload().getSubject());
    assertNotNull(jwt.getPayload().getUserId());
    assertNotNull(jwt.getPayload().getUsername());
  }

  @Test
  public void testConstructorWithArrayAud() {
    JsonWebToken jwt = new JsonWebToken(encodedTokenWithArrayAud);
    assertNotNull(jwt.getPayload());
    assertNotNull(jwt.getPayload().getAudiences());
    assertEquals(jwt.getPayload().getAudiences().size(), 1);
    assertEquals(jwt.getPayload().getAudiences().get(0), "account-iam.platform.saas.ibm.com");
    // getAudience() should return the first element for backward compatibility
    assertEquals(jwt.getPayload().getAudience(), "account-iam.platform.saas.ibm.com");
    assertNotNull(jwt.getPayload().getExpiresAt());
    assertNotNull(jwt.getPayload().getIssuedAt());
  }
}
