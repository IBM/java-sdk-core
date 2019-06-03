package com.ibm.cloud.sdk.core.test.security;

import org.junit.Test;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthConfig;

import static org.junit.Assert.assertEquals;

public class BasicAuthConfigTest {

  @Test
  public void testBuildSuccess() {
    String username = "test-username";
    String password = "test-password";

    BasicAuthConfig config = new BasicAuthConfig.Builder()
        .username(username)
        .password(password)
        .build();

    assertEquals(username, config.getUsername());
    assertEquals(password, config.getPassword());
    assertEquals(Authenticator.AUTHTYPE_BASIC, config.authenticationType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildInvalid() {
    String username = "{test-username}";
    String password = "test-password";

    BasicAuthConfig config = new BasicAuthConfig.Builder()
        .username(username)
        .password(password)
        .build();
  }
}
