package com.ibm.cloud.sdk.core.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataAuthenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;

import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;

public class AuthenticationTest {
  private static final String APIKEY = "12345";
  private static final String BASIC_USERNAME = "basicUser";

  public class TestService extends BaseService {
    private static final String SERVICE_NAME = "service1";

    public TestService() {
      this(null);
    }

    public TestService(Authenticator authenticator) {
      this(SERVICE_NAME, authenticator);
    }

    public TestService(String name, Authenticator authenticator) {
      super(name,
        (authenticator != null ? authenticator : ConfigBasedAuthenticatorFactory.getAuthenticator(name)));
    }
  }

  @Test
  public void authenticateIAM() {
    IamAuthenticator auth = new IamAuthenticator(APIKEY);
    TestService service = new TestService(auth);
    assertEquals(Authenticator.AUTHTYPE_IAM, service.getAuthenticator().authenticationType());
  }

  @Test
  public void authenticateBasicAuth() {
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService service = new TestService(auth);
    assertEquals(Authenticator.AUTHTYPE_BASIC, service.getAuthenticator().authenticationType());
  }

  @Test
  public void authenticateNoauth() {
    NoAuthAuthenticator auth = new NoAuthAuthenticator();
    TestService service = new TestService(auth);
    assertEquals(Authenticator.AUTHTYPE_NOAUTH, service.getAuthenticator().authenticationType());
  }

  @Test
  public void authenticateCP4D() {
    CloudPakForDataAuthenticator auth = new CloudPakForDataAuthenticator("/my/icp4d/url", "icp4d_user", "password1");
    TestService service = new TestService(auth);
    assertEquals(Authenticator.AUTHTYPE_CP4D, service.getAuthenticator().authenticationType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoAuthenticator() {
    new TestService();
  }

  @Test
  public void testCredentialFileAuthenticator() {
    TestService service = new TestService("natural_language_classifier", null);
    assertNotNull(service.getAuthenticator());
    assertEquals(Authenticator.AUTHTYPE_IAM, service.getAuthenticator().authenticationType());
    assertEquals("https://gateway.watsonplatform.net/natural-language-classifier/api", service.getEndPoint());
    OkHttpClient client = service.getClient();
    assertNotNull(client);
    assertFalse(client.hostnameVerifier() instanceof OkHostnameVerifier);
  }
}
