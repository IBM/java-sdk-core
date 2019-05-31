package com.ibm.cloud.sdk.core.test.security;

import com.ibm.cloud.sdk.security.Authenticator;
import com.ibm.cloud.sdk.security.basicauth.BasicAuthConfig;
import org.junit.Test;

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
