/**
 * (C) Copyright IBM Corp. 2015, 2021.
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

package com.ibm.cloud.sdk.core.test.security;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BearerTokenAuthenticator;

import okhttp3.Request;

@SuppressWarnings("deprecation")
public class BearerTokenAuthenticatorTest {

  @Test
  public void testSuccess() {

    // Initial access token.
    String bearerToken = "abcdef-123456";

    BearerTokenAuthenticator auth = new BearerTokenAuthenticator(bearerToken);
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, auth.authenticationType());

    Request.Builder requestBuilder;
    Request request;

    // Simulate a Request and check the Authorization header added to it.
    requestBuilder = new Request.Builder().url("https://test.com");
    auth.authenticate(requestBuilder);
    request = requestBuilder.build();
    assertNotNull(request.header(HttpHeaders.AUTHORIZATION));
    assertEquals("Bearer " + bearerToken, request.header(HttpHeaders.AUTHORIZATION));

    // Now change the authenticator's access token value and re-check.
    bearerToken = "new-access-token";
    auth.setBearerToken(bearerToken);
    requestBuilder = new Request.Builder().url("https://test.com");
    auth.authenticate(requestBuilder);
    request = requestBuilder.build();
    assertNotNull(request.header(HttpHeaders.AUTHORIZATION));
    assertEquals("Bearer " + bearerToken, request.header(HttpHeaders.AUTHORIZATION));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingAccessToken() {
    new BearerTokenAuthenticator((String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyAccessToken() {
    new BearerTokenAuthenticator("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingAccessTokenMap() {
    Map<String, String> props = new HashMap<>();
    new BearerTokenAuthenticator(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyAccessTokenMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "");
    new BearerTokenAuthenticator(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingAccessToken() {
    Map<String, String> props = new HashMap<>();
    BearerTokenAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyAccessToken() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "");
    BearerTokenAuthenticator.fromConfiguration(props);
  }

  public void testCtorCorrectConfig() {
    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator("my-access-token");
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, authenticator.authenticationType());
    assertEquals("my-access-token", authenticator.getBearerToken());
  }

  public void testConfigCorrectConfig() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "my-access-token");
    BearerTokenAuthenticator authenticator = BearerTokenAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, authenticator.authenticationType());
    assertEquals("my-access-token", authenticator.getBearerToken());
  }

  @Test
  public void testSingleAuthHeader() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "my-access-token");
    BearerTokenAuthenticator auth = BearerTokenAuthenticator.fromConfiguration(props);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    auth.authenticate(requestBuilder);
    // call authenticate twice on the same request
    auth.authenticate(requestBuilder);
    Request request = requestBuilder.build();

    List<String> authHeaders = request.headers(HttpHeaders.AUTHORIZATION);
    assertEquals(authHeaders.size(), 1);
  }
}
