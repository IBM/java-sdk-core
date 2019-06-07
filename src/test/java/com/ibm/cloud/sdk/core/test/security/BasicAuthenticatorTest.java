package com.ibm.cloud.sdk.core.test.security;

import com.google.common.io.BaseEncoding;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthConfig;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthenticator;

import okhttp3.Request;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class BasicAuthenticatorTest {

  @Test
  public void testAuthenticate() {
    String username = "test-username";
    String password = "test-password";

    BasicAuthConfig config = new BasicAuthConfig.Builder()
        .username(username)
        .password(password)
        .build();
    BasicAuthenticator authenticator = new BasicAuthenticator(config);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    Request request = requestBuilder.build();

    String authHeader = request.header(HttpHeaders.AUTHORIZATION);
    System.out.println(authHeader);
    assertNotNull(authHeader);
    assertEquals("Basic " + BaseEncoding.base64().encode((username + ":" + password).getBytes()), authHeader);
  }
}
