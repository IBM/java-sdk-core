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
    private static final String SERVICE_NAME = "service-1";

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

    public void configureSvc(String serviceName) {
      configureService(serviceName);
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
    service.configureSvc("natural_language_classifier");
    assertNotNull(service.getAuthenticator());
    assertEquals(Authenticator.AUTHTYPE_IAM, service.getAuthenticator().authenticationType());
    assertEquals("https://gateway.watsonplatform.net/natural-language-classifier/api", service.getServiceUrl());
    OkHttpClient client = service.getClient();
    assertNotNull(client);
    assertFalse(client.hostnameVerifier() instanceof OkHostnameVerifier);
  }
}
