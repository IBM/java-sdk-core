package com.ibm.cloud.sdk.core.test.service;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.AuthenticatorConfig;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthConfig;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthenticator;
import com.ibm.cloud.sdk.core.security.icp4d.ICP4DAuthenticator;
import com.ibm.cloud.sdk.core.security.icp4d.ICP4DConfig;
import com.ibm.cloud.sdk.core.security.noauth.NoauthAuthenticator;
import com.ibm.cloud.sdk.core.security.noauth.NoauthConfig;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.cloud.sdk.core.service.security.IamTokenManager;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class AuthenticationTest {
  private static final String APIKEY_USERNAME = "apikey";
  private static final String APIKEY = "12345";
  private static final String ICP_APIKEY = "icp-12345";
  private static final String BASIC_USERNAME = "basicUser";

  public class TestService extends BaseService {
    private static final String SERVICE_NAME = "test";

    public TestService() {
      super(SERVICE_NAME);
    }

    public TestService(AuthenticatorConfig config) {
      super(SERVICE_NAME);
      setAuthenticator(config);
    }

    public Authenticator authenticator() {
      return this.getAuthenticator();
    }
  }

  @Test
  public void authenticateIam() {
    IamOptions options = new IamOptions.Builder()
        .apiKey(APIKEY)
        .build();
    TestService service = new TestService(options);
    assertTrue(service.isTokenManagerSet());
    assertTrue(service.authenticator() instanceof IamTokenManager);
  }

  @Test
  public void authenticateIamWithApiKeyAsUsername() {
    TestService service = new TestService();
    service.setUsernameAndPassword(APIKEY_USERNAME, APIKEY);
    assertTrue(service.isTokenManagerSet());
    assertTrue(service.authenticator() instanceof IamTokenManager);
  }

  @Test
  public void authenticateIcpBasicAuth() {
    TestService service = new TestService();
    service.setUsernameAndPassword(APIKEY_USERNAME, ICP_APIKEY);
    assertFalse(service.isTokenManagerSet());
    assertTrue(service.authenticator() instanceof BasicAuthenticator);
  }

  @Test
  public void authenticateBasicAuth() {
    BasicAuthConfig config = new BasicAuthConfig.Builder()
        .username(BASIC_USERNAME)
        .password("password1")
        .build();
    TestService service = new TestService(config);
    assertTrue(service.authenticator() instanceof BasicAuthenticator);
    service.setSkipAuthentication(false);
    assertTrue(service.authenticator() instanceof BasicAuthenticator);
  }

  @Test
  public void authenticateNone() {
    NoauthConfig config = new NoauthConfig();
    TestService service = new TestService(config);
    assertTrue(service.authenticator() instanceof NoauthAuthenticator);
    assertTrue(service.isSkipAuthentication());
  }

  @Test
  public void authenticateICP4D() {
    ICP4DConfig config = new ICP4DConfig.Builder()
        .username("icp4d_user")
        .password("password1")
        .url("/my/icp4d/url")
        .build();
    TestService service = new TestService(config);
    assertTrue(service.authenticator() instanceof ICP4DAuthenticator);
  }

  @Test
  public void testSkipAuthentication() {
    TestService service = new TestService();
    service.setSkipAuthentication(true);
    assertTrue(service.authenticator() instanceof NoauthAuthenticator);
    assertTrue(service.isSkipAuthentication());
  }

  @Test
  public void testDefaultServiceCtor() {
    TestService service = new TestService();
    assertFalse(service.isSkipAuthentication());
    assertNull(service.authenticator());
  }

  @Test
  public void testSetAuthenticator() {
    // Test use of setAuthenticator() on existing service instance.
    TestService service = new TestService();
    assertFalse(service.isSkipAuthentication());
    assertNull(service.authenticator());

    service.setAuthenticator(new NoauthConfig());
    assertTrue(service.authenticator() instanceof NoauthAuthenticator);
    assertTrue(service.isSkipAuthentication());

    service.setAuthenticator(null);
    assertNull(service.authenticator());
    assertFalse(service.isSkipAuthentication());

    BasicAuthConfig baConfig = new BasicAuthConfig.Builder()
        .username("user")
        .password("pw")
        .build();
    service.setAuthenticator(baConfig);
    assertTrue(service.authenticator() instanceof BasicAuthenticator);
    assertFalse(service.isSkipAuthentication());

    IamOptions iamConfig = new IamOptions.Builder()
        .apiKey("myapikey")
        .build();
    service.setAuthenticator(iamConfig);
    assertTrue(service.authenticator() instanceof IamTokenManager);
    assertFalse(service.isSkipAuthentication());

    ICP4DConfig icp4dConfig = new ICP4DConfig.Builder()
        .userManagedAccessToken("myuseraccesstoken")
        .build();
    service.setAuthenticator(icp4dConfig);
    assertTrue(service.authenticator() instanceof ICP4DAuthenticator);
    assertFalse(service.isSkipAuthentication());
  }

  @Test
  public void multiAuthenticationWithMultiBindSameServiceOnVcapService() {

    CredentialUtils.setServices("{\n"
        + "  \"test\": [\n"
        + "    {\n"
        + "      \"credentials\": {\n"
        + "        \"apikey\": \"" + APIKEY + "\",\n"
        + "        \"url\": \"https://gateway.watsonplatform.net/discovery/api\"\n"
        + "      },\n"
        + "      \"plan\": \"lite\"\n"
        + "    }\n"
        + "  ]\n"
        + "}\n");

    TestService serviceA = new TestService();
    serviceA.setUsernameAndPassword(APIKEY_USERNAME, APIKEY);

    TestService serviceB = new TestService();
    serviceB.setUsernameAndPassword(BASIC_USERNAME, APIKEY);

    assertTrue(serviceA.isTokenManagerSet());
    assertFalse(serviceB.isTokenManagerSet());
  }
}
