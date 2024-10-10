/**
 * (C) Copyright IBM Corp. 2015, 2024.
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataServiceAuthenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataServiceInstanceAuthenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.ContainerAuthenticator;
import com.ibm.cloud.sdk.core.security.IamAssumeAuthenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.MCSPAuthenticator;
import com.ibm.cloud.sdk.core.security.VpcInstanceAuthenticator;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;

/**
 * This class tests the ConfigBasedAuthenticatorFactory class.
 * We'll using various mocking techniques to simulate the credential file, environment and vcap services.
 */
public class ConfigBasedAuthenticatorFactoryTest {
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

    env.put("SERVICE8_AUTH_TYPE", Authenticator.AUTHTYPE_VPC);
    env.put("SERVICE8_AUTH_URL", "https://vpc.imds.com/api");
    env.put("SERVICE8_IAM_PROFILE_CRN", "crn:iam-profile-1");

    env.put("SERVICE9_AUTH_URL", "https://service9/zen-data/v3/service_instances/serviceInstanceId/token");
    env.put("SERVICE9_DISABLE_SSL", "false");
    env.put("SERVICE9_AUTHTYPE", "Cp4DServiceInstance");
    env.put("SERVICE9_USERNAME", "my-cp4d-user");
    env.put("SERVICE9_APIKEY", "my-cp4d-apikey");
    env.put("SERVICE9_SERVICE_INSTANCE_ID", "my-cp4d-service-instance-id");
    env.put("SERVICE9_AUTH_DISABLE_SSL", "false");

    env.put("SERVICE10_AUTH_TYPE", "mCsP");
    env.put("SERVICE10_AUTH_URL", "https://mcsp.ibm.com");
    env.put("SERVICE10_APIKEY", "my-api-key");

    env.put("SERVICE11_AUTH_TYPE", "iamASsumE");
    env.put("SERVICE11_APIKEY", "my-api-key");
    env.put("SERVICE11_IAM_PROFILE_CRN", "my-profile-crn-1");

    env.put("SERVICE12_AUTH_TYPE", "iamAssume");
    env.put("SERVICE12_APIKEY", "my-api-key");
    env.put("SERVICE12_IAM_PROFILE_ID", "my-profile-id-1");

    env.put("SERVICE13_AUTH_TYPE", "IAMassume");
    env.put("SERVICE13_APIKEY", "my-api-key");
    env.put("SERVICE13_IAM_PROFILE_NAME", "my-profile-1");
    env.put("SERVICE13_IAM_ACCOUNT_ID", "my-account-id-1");

    env.put("ERROR1_AUTH_TYPE", Authenticator.AUTHTYPE_CP4D);
    env.put("ERROR2_AUTH_TYPE", "BAD_AUTH_TYPE");

    return env;
  }

  // This will be our mocked version of the EnvironmentUtils class.
  private static MockedStatic<EnvironmentUtils> envMock = null;

  @BeforeMethod
  public void createEnvMock() {
    envMock = Mockito.mockStatic(EnvironmentUtils.class);
  }

  @AfterMethod
  public void clearEnvMock() {
    if (envMock != null) {
      envMock.close();
      envMock = null;
    }
  }

  /**
   * Sets up a mock VCAP_SERVICES object.
   */
  public void setupVCAP() {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(VCAP_SERVICES);
    final String vcapServices = getStringFromInputStream(in);

    envMock.when(() ->EnvironmentUtils.getenv("VCAP_SERVICES")).thenReturn(vcapServices);
  }

  @Test
  public void testNoConfig() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(null);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNull(auth);
  }

  /**
   * This class is a subclass of our authenticator factory and is
   * simply a test to make sure that the subclass usecase works as expected.
   */
  public static class TestAuthFactorySubclass extends ConfigBasedAuthenticatorFactory {

    public static Authenticator getAuthenticator(String serviceName) {
      // For testing purposes, just hard-code the service properties
      // so we can simulate an alternate config source.
      Map<String, String> authProps = new HashMap<>();
      authProps.put(Authenticator.PROPNAME_AUTH_TYPE, "basic");
      authProps.put(Authenticator.PROPNAME_USERNAME, "myuser");
      authProps.put(Authenticator.PROPNAME_PASSWORD, "mypassword");

      Authenticator authenticator = createAuthenticator(authProps);

      return authenticator;
    }
  }

  @Test
  public void testFactorySubclass() {

    Authenticator auth = TestAuthFactorySubclass.getAuthenticator("dont_care");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_BASIC);
  }

  @Test
  public void testFileCredentialsService1() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"), ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM);
  }

  @Test
  public void testFileCredentialsService2() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service2");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_BASIC);
  }

  @Test
  public void testFileCredentialsService3() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service3");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CP4D);
  }

  @Test
  public void testFileCredentialsService4() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service4");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_NOAUTH);
  }

  @Test
  public void testFileCredentialsService5() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service5");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_BEARER_TOKEN);
  }

  @Test
  public void testFileCredentialsService11() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

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
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

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
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

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
  public void testFileCredentialsService15() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_15");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_VPC);
    VpcInstanceAuthenticator containerAuth = (VpcInstanceAuthenticator) auth;
    assertEquals(containerAuth.getIamProfileCrn(), "crn:iam-profile-1");
    assertNull(containerAuth.getIamProfileId());
    assertEquals(containerAuth.getURL(), "https://vpc.imds.com/api");
  }

  @Test
  public void testFileCredentialsService16() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_16");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_VPC);
    VpcInstanceAuthenticator containerAuth = (VpcInstanceAuthenticator) auth;
    assertNull(containerAuth.getIamProfileCrn());
    assertEquals(containerAuth.getIamProfileId(), "iam-profile-1-id");
    assertNull(containerAuth.getURL());
  }

  @Test
  public void testFileCredentialsService17() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_17");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_MCSP);
    MCSPAuthenticator mcspAuth = (MCSPAuthenticator) auth;
    assertEquals(mcspAuth.getApiKey(), "my-api-key");
    assertEquals(mcspAuth.getURL(), "https://mcsp.ibm.com");
  }

  @Test
  public void testFileCredentialsService18() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_18");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertEquals(iamAuth.getIamProfileCrn(), "my-profile-crn-1");
    assertNull(iamAuth.getIamProfileId());
    assertNull(iamAuth.getIamProfileName());
    assertNull(iamAuth.getIamAccountId());
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
  }

  @Test
  public void testFileCredentialsService19() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_19");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertNull(iamAuth.getIamProfileCrn());
    assertEquals(iamAuth.getIamProfileId(), "my-profile-id-1");
    assertNull(iamAuth.getIamProfileName());
    assertNull(iamAuth.getIamAccountId());
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
  }

  @Test
  public void testFileCredentialsService20() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service_20");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertNull(iamAuth.getIamProfileCrn());
    assertNull(iamAuth.getIamProfileId());
    assertEquals(iamAuth.getIamProfileName(), "my-profile-1");
    assertEquals(iamAuth.getIamAccountId(), "my-account-id-1");
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError1() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError2() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError3() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFileCredentialsError4() {
    envMock.when(() ->EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);

    ConfigBasedAuthenticatorFactory.getAuthenticator("error4");
  }

  @Test
  public void testFileCredentialsSystemPropService1() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(System.getProperty("IBM_CREDENTIALS_FILE"), ALTERNATE_CRED_FILENAME);

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM);
    System.clearProperty("IBM_CREDENTIALS_FILE");
  }

  @Test
  public void testEnvCredentialsService1() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service-1");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM);
    IamAuthenticator iamAuth = (IamAuthenticator) auth;
    assertEquals(iamAuth.getApiKey(), "my-api-key");
    assertEquals(iamAuth.getClientId(), "my-client-id");
    assertEquals(iamAuth.getClientSecret(), "my-client-secret");
    assertEquals(iamAuth.getURL(), "https://iamhost/iam/api");
    assertTrue(iamAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService6() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service6");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CP4D_SERVICE);
    CloudPakForDataServiceAuthenticator cp4dAuth = (CloudPakForDataServiceAuthenticator) auth;
    assertEquals(cp4dAuth.getURL(), "https://service1/zen-data/internal");
    assertEquals(cp4dAuth.getServiceBrokerSecret(), "f8b7czjt701wy6253be5q8ad8f07kd08");
    assertTrue(cp4dAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService7() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service7");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
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

  @Test
  public void testEnvCredentialsService8() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service8");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_VPC);
    VpcInstanceAuthenticator containerAuth = (VpcInstanceAuthenticator) auth;
    assertEquals(containerAuth.getIamProfileCrn(), "crn:iam-profile-1");
    assertNull(containerAuth.getIamProfileId());
    assertEquals(containerAuth.getURL(), "https://vpc.imds.com/api");
  }

  @Test
  public void testEnvCredentialsService9() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service9");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE);
    CloudPakForDataServiceInstanceAuthenticator cp4dAuth = (CloudPakForDataServiceInstanceAuthenticator) auth;
    assertEquals(cp4dAuth.getURL(), "https://service9/zen-data/v3/service_instances/serviceInstanceId/token");
    assertEquals(cp4dAuth.getUsername(), "my-cp4d-user");
    assertEquals(cp4dAuth.getApikey(), "my-cp4d-apikey");
    assertEquals(cp4dAuth.getServiceInstanceId(), "my-cp4d-service-instance-id");
    assertFalse(cp4dAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService10() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service10");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_MCSP);
    MCSPAuthenticator mcspAuth = (MCSPAuthenticator) auth;
    assertEquals(mcspAuth.getURL(), "https://mcsp.ibm.com");
    assertEquals(mcspAuth.getApiKey(), "my-api-key");
    assertFalse(mcspAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService11() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service11");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
    assertEquals(iamAuth.getIamProfileCrn(), "my-profile-crn-1");
    assertNull(iamAuth.getIamProfileId());
    assertNull(iamAuth.getIamProfileName());
    assertNull(iamAuth.getIamAccountId());
    assertFalse(iamAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService12() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service12");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
    assertNull(iamAuth.getIamProfileCrn());
    assertEquals(iamAuth.getIamProfileId(), "my-profile-id-1");
    assertNull(iamAuth.getIamProfileName());
    assertNull(iamAuth.getIamAccountId());
    assertFalse(iamAuth.getDisableSSLVerification());
  }

  @Test
  public void testEnvCredentialsService13() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("service13");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    IamAssumeAuthenticator iamAuth = (IamAssumeAuthenticator) auth;
    assertEquals(iamAuth.getURL(), "https://iam.cloud.ibm.com");
    assertNull(iamAuth.getIamProfileCrn());
    assertNull(iamAuth.getIamProfileId());
    assertEquals(iamAuth.getIamProfileName(), "my-profile-1");
    assertEquals(iamAuth.getIamAccountId(), "my-account-id-1");
    assertFalse(iamAuth.getDisableSSLVerification());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEnvCredentialsError1() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    ConfigBasedAuthenticatorFactory.getAuthenticator("error1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEnvCredentialsError2() {
    envMock.when(() ->EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    ConfigBasedAuthenticatorFactory.getAuthenticator("error2");
  }

  @Test
  public void testVcapCredentialsDiscovery() {
    setupVCAP();

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("discovery");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_BASIC);
  }

  @Test
  public void testVcapCredentialsLT() {
    setupVCAP();

    Authenticator auth = ConfigBasedAuthenticatorFactory.getAuthenticator("language_translator");
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM);
  }
}
