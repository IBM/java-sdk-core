/**
 * (C) Copyright IBM Corp. 2015, 2021.
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

import static com.ibm.cloud.sdk.core.test.TestUtils.getStringFromInputStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataServiceAuthenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.ContainerAuthenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;

/**
 * This class tests the ConfigBasedAuthenticatorFactory class.
 * We'll using various mocking techniques to simulate the credential file, environment and vcap services.
 */
@PrepareForTest({ EnvironmentUtils.class })
public class ConfigBasedAuthenticatorFactoryTest extends PowerMockTestCase {
  private static final String ALTERNATE_CRED_FILENAME = "src/test/resources/my-credentials.env";
  private static final String VCAP_SERVICES = "vcap_services.json";

  // Creates a mock set of environment variables that are returned by EnvironmentUtils.getenv().
  private Map<String, String> getTestProcessEnvironment() {
    Map<String, String> env = new HashMap<>();
    env.put("SERVICE_1_URL", "https://service1/api");
    env.put("SERVICE_1_DISABLE_SSL", "true");
    env.put("SERVICE_1_AUTH_TYPE", "IaM");
    env.put("SERVICE_1_APIKEY", "my-api-key");
    env.put("SERVICE_1_CLIENT_ID", "my-client-id");
    env.put("SERVICE_1_CLIENT_SECRET", "my-client-secret");
    env.put("SERVICE_1_AUTH_URL", "https://iamhost/iam/api");
    env.put("SERVICE_1_AUTH_DISABLE_SSL", "true");

    env.put("SERVICE2_URL", "https://service2/api");
    env.put("SERVICE2_DISABLE_SSL", "false");
    env.put("SERVICE2_AUTHTYPE", Authenticator.AUTHTYPE_BASIC);
    env.put("SERVICE2_USERNAME", "my-user");
    env.put("SERVICE2_PASSWORD", "my-password");

    env.put("SERVICE3_URL", "https://service3/api");
    env.put("SERVICE3_DISABLE_SSL", "false");
    env.put("SERVICE3_AUTHTYPE", "Cp4D");
    env.put("SERVICE3_AUTH_URL", "https://cp4dhost/cp4d/api");
    env.put("SERVICE3_USERNAME", "my-cp4d-user");
    env.put("SERVICE3_PASSWORD", "my-cp4d-password");
    env.put("SERVICE3_AUTH_DISABLE_SSL", "false");

    env.put("SERVICE4_URL", "https://service4/api");
    env.put("SERVICE4_DISABLE_SSL", "false");
    env.put("SERVICE4_AUTH_TYPE", Authenticator.AUTHTYPE_NOAUTH);

    env.put("SERVICE5_URL", "https://service5/api");
    env.put("SERVICE5_DISABLE_SSL", "true");
    env.put("SERVICE5_AUTH_TYPE", Authenticator.AUTHTYPE_BEARER_TOKEN);
    env.put("SERVICE5_BEARER_TOKEN", "my-bearer-token");

    env.put("SERVICE6_AUTH_URL", "https://service1/zen-data/internal");
    env.put("SERVICE6_AUTH_DISABLE_SSL", "true");
    env.put("SERVICE6_AUTH_TYPE", Authenticator.AUTHTYPE_CP4D_SERVICE);
    env.put("SERVICE6_SERVICE_BROKER_SECRET", "f8b7czjt701wy6253be5q8ad8f07kd08");

    env.put("SERVICE7_AUTH_URL", "https://iam.com/api");
    env.put("SERVICE7_CR_TOKEN_FILENAME", "cr-token.txt");
    env.put("SERVICE7_IAM_PROFILE_NAME", "iam-user1");
    env.put("SERVICE7_IAM_PROFILE_ID", "iam-id1");
    env.put("SERVICE7_CLIENT_ID", "my-client-id");
    env.put("SERVICE7_CLIENT_SECRET", "my-client-secret");
    env.put("SERVICE7_SCOPE", "admin user viewer");

    env.put("ERROR1_AUTH_TYPE", Authenticator.AUTHTYPE_CP4D);
    env.put("ERROR2_AUTH_TYPE", "BAD_AUTH_TYPE");

    return env;
  }

  /**
   * Sets up a mock VCAP_SERVICES object.
   */
  public void setupVCAP() {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(VCAP_SERVICES);
    final String vcapServices = getStringFromInputStream(in);

    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("VCAP_SERVICES")).thenReturn(vcapServices);
  }

  @Test
  public void testNoConfig() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(null);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNull(auth);
  }

  @Test
  public void testFileCredentialsService1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_IAM, auth.authenticationType());
  }

  @Test
  public void testFileCredentialsService2() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service2");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_BASIC, auth.authenticationType());
  }

  @Test
  public void testFileCredentialsService3() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service3");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_CP4D, auth.authenticationType());
  }

  @Test
  public void testFileCredentialsService4() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service4");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_NOAUTH, auth.authenticationType());
  }

  @Test
  public void testFileCredentialsService11() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_11");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    ContainerAuthenticator containerAuth = (ContainerAuthenticator) auth;
    assertEquals(containerAuth.getCrTokenFilename(), "cr-token.txt");
    assertEquals(containerAuth.getIamProfileName(), "iam-user1");
    assertEquals(containerAuth.getIamProfileId(), "iam-id1");
    assertEquals(containerAuth.getURL(), "https://iam.com");
    assertEquals(containerAuth.getScope(), "scope1 scope2");
    assertEquals(containerAuth.getClientId(), "my-client-id");
    assertEquals(containerAuth.getClientSecret(), "my-client-secret");
    assertTrue(containerAuth.getDisableSSLVerification());
  }

  @Test
  public void testFileCredentialsService12() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_12");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    ContainerAuthenticator containerAuth = (ContainerAuthenticator) auth;
    assertNull(containerAuth.getCrTokenFilename());
    assertEquals(containerAuth.getIamProfileName(), "iam-user1");
    assertNull(containerAuth.getIamProfileId());
    assertNotNull(containerAuth.getURL());
    assertNull(containerAuth.getScope());
    assertNull(containerAuth.getClientId());
    assertNull(containerAuth.getClientSecret());
    assertFalse(containerAuth.getDisableSSLVerification());
  }

  @Test
  public void testFileCredentialsService13() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_13");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    ContainerAuthenticator containerAuth = (ContainerAuthenticator) auth;
    assertNull(containerAuth.getCrTokenFilename());
    assertNull(containerAuth.getIamProfileName());
    assertEquals(containerAuth.getIamProfileId(), "iam-id1");
    assertNotNull(containerAuth.getURL());
    assertNull(containerAuth.getScope());
    assertNull(containerAuth.getClientId());
    assertNull(containerAuth.getClientSecret());
    assertFalse(containerAuth.getDisableSSLVerification());
  }

  @Test
  public void testFileCredentialsService5() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service5");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, auth.authenticationType());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError2() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError3() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError4() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error4");
  }

  @Test
  public void testFileCredentialsSystemPropService1() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_IAM, auth.authenticationType());
    System.clearProperty("IBM_CREDENTIALS_FILE");
  }

  @Test
  public void testEnvCredentialsService1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_IAM, auth.authenticationType());
    IamAuthenticator iamAuth = (IamAuthenticator) auth;
    assertEquals(iamAuth.getApiKey(), "my-api-key");
    assertEquals(iamAuth.getClientId(), "my-client-id");
    assertEquals(iamAuth.getClientSecret(), "my-client-secret");
    assertEquals(iamAuth.getURL(), "https://iamhost/iam/api");
    assertTrue(iamAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService6() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service6");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, auth.authenticationType());
    CloudPakForDataServiceAuthenticator cp4dAuth = (CloudPakForDataServiceAuthenticator) auth;
    assertEquals(cp4dAuth.getURL(), "https://service1/zen-data/internal");
    assertEquals(cp4dAuth.getServiceBrokerSecret(), "f8b7czjt701wy6253be5q8ad8f07kd08");
    assertTrue(cp4dAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService7() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service7");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_CONTAINER, auth.authenticationType());
    ContainerAuthenticator containerAuth = (ContainerAuthenticator) auth;
    assertEquals(containerAuth.getURL(), "https://iam.com/api");
    assertEquals(containerAuth.getCrTokenFilename(), "cr-token.txt");
    assertEquals(containerAuth.getIamProfileName(), "iam-user1");
    assertEquals(containerAuth.getIamProfileId(), "iam-id1");
    assertEquals(containerAuth.getClientId(), "my-client-id");
    assertEquals(containerAuth.getClientSecret(), "my-client-secret");
    assertEquals(containerAuth.getScope(), "admin user viewer");
    assertFalse(containerAuth.getDisableSSLVerification());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEnvCredentialsError1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    ConfigBasedAuthenticatorFactory.getAuthenticator("error1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEnvCredentialsError2() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    ConfigBasedAuthenticatorFactory.getAuthenticator("error2");
  }

  @Test
  public void testVcapCredentialsDiscovery() {
    setupVCAP();

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("discovery");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_BASIC, auth.authenticationType());
  }

  @Test
  public void testVcapCredentialsLT() {
    setupVCAP();

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("language_translator");
    assertNotNull(auth);
    assertEquals(Authenticator.AUTHTYPE_IAM, auth.authenticationType());
  }
}
