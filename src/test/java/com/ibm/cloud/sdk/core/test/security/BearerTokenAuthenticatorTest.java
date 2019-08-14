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
