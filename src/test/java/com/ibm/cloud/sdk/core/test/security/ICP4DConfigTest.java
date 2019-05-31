package com.ibm.cloud.sdk.core.test.security;

import com.ibm.cloud.sdk.security.Authenticator;
import com.ibm.cloud.sdk.security.icp4d.ICP4DConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ICP4DConfigTest {

  @Test
  public void testBuildSuccess() {
    String url = "https://test.com";
    String username = "test-username";
    String password = "test-password";
    String accessToken = "test-token";

    ICP4DConfig config = new ICP4DConfig.Builder()
        .url(url)
        .username(username)
        .password(password)
        .userManagedAccessToken(accessToken)
        .disableSSLVerification(true)
        .build();

    assertEquals(url, config.getUrl());
    assertEquals(username, config.getUsername());
    assertEquals(password, config.getPassword());
    assertEquals(accessToken, config.getUserManagedAccessToken());
    assertTrue(config.isDisableSSLVerification());
    assertEquals(Authenticator.AUTHTYPE_ICP4D, config.authenticationType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildInvalid() {
    ICP4DConfig config = new ICP4DConfig.Builder()
        .username("test-username")
        .build();
  }
}
