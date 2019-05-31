package com.ibm.cloud.sdk.core.test.security;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.cloud.sdk.core.service.security.IamTokenManager;
import com.ibm.cloud.sdk.security.Authenticator;
import com.ibm.cloud.sdk.security.AuthenticatorConfig;
import com.ibm.cloud.sdk.security.AuthenticatorFactory;
import com.ibm.cloud.sdk.security.basicauth.BasicAuthConfig;
import com.ibm.cloud.sdk.security.basicauth.BasicAuthenticator;
import com.ibm.cloud.sdk.security.icp4d.ICP4DAuthenticator;
import com.ibm.cloud.sdk.security.icp4d.ICP4DConfig;
import com.ibm.cloud.sdk.security.noauth.NoauthAuthenticator;
import com.ibm.cloud.sdk.security.noauth.NoauthConfig;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AuthenticatorFactoryTest {

  private class UnknownConfig implements AuthenticatorConfig {

    @Override
    public String authenticationType() {
      return "unknown";
    }

    @Override
    public void validate() throws IllegalArgumentException { }
  }

  @Test
  public void testGetAuthenticatorIam() {
    IamOptions config = new IamOptions.Builder()
        .apiKey("test-key")
        .build();
    Authenticator authenticator = AuthenticatorFactory.getAuthenticator(config);
    assertTrue(authenticator instanceof IamTokenManager);
  }

  @Test
  public void testGetAuthenticatorIcp4d() {
    ICP4DConfig config = new ICP4DConfig.Builder()
        .url("https://test.com")
        .userManagedAccessToken("test-token")
        .build();
    Authenticator authenticator = AuthenticatorFactory.getAuthenticator(config);
    assertTrue(authenticator instanceof ICP4DAuthenticator);
  }

  @Test
  public void testGetAuthenticatorBasic() {
    BasicAuthConfig config = new BasicAuthConfig.Builder()
        .username("test-username")
        .password("test-password")
        .build();
    Authenticator authenticator = AuthenticatorFactory.getAuthenticator(config);
    assertTrue(authenticator instanceof BasicAuthenticator);
  }

  @Test
  public void testGetAuthenticatorNoauth() {
    Authenticator authenticator = AuthenticatorFactory.getAuthenticator(new NoauthConfig());
    assertTrue(authenticator instanceof NoauthAuthenticator);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAuthenticatorUnknown() {
    AuthenticatorFactory.getAuthenticator(new UnknownConfig());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAuthenticatorInvalidConfig() {
    AuthenticatorFactory.getAuthenticator(new IamOptions());
  }
}
