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

package com.ibm.cloud.sdk.core.test.security;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BearerTokenAuthenticator;

import okhttp3.Request;

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

  @Test(expected = IllegalArgumentException.class)
  public void testMissingAccessToken() {
    new BearerTokenAuthenticator((String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAccessToken() {
    new BearerTokenAuthenticator("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingAccessTokenMap() {
    Map<String, String> props = new HashMap<>();
    new BearerTokenAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAccessTokenMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "");
    new BearerTokenAuthenticator(props);
  }

  public void testCorrectConfig() {
    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator("my-access-token");
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, authenticator.authenticationType());
    assertEquals("my-access-token", authenticator.getBearerToken());
  }

  public void testCorrectConfigMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_BEARER_TOKEN, "my-access-token");
    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, authenticator.authenticationType());
    assertEquals("my-access-token", authenticator.getBearerToken());
  }
}
