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

package com.ibm.cloud.sdk.core.util;

import static com.ibm.cloud.sdk.core.test.TestUtils.getStringFromInputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;

/**
 * The Class CredentialUtilsTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EnvironmentUtils.class })
public class CredentialUtilsTest {
  private static final String ALTERNATE_CRED_FILENAME = "src/test/resources/my-credentials.env";
  private static final String VCAP_SERVICES = "vcap_services.json";
  private static final String NOT_A_USERNAME = "not-a-username";
  private static final String NOT_A_PASSWORD = "not-a-password";

  private Map<String, String> getTestProcessEnvironment() {
    Map<String, String> env = new HashMap<>();
    env.put("SERVICE_1_URL", "https://service1/api");
    env.put("SERVICE_1_DISABLE_SSL", "true");
    env.put("SERVICE2_URL", "https://service2/api");
    env.put("SERVICE2_DISABLE_SSL", "false");
    env.put("SERVICE3_URL", "https://service3/api");
    env.put("SERVICE3_DISABLE_SSL", "false");
    env.put("SERVICE4_URL", "https://service4/api");
    env.put("SERVICE4_DISABLE_SSL", "false");
    env.put("SERVICE5_URL", "https://service5/api");
    env.put("SERVICE5_DISABLE_SSL", "true");
    env.put("SERVICE_1_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    env.put("SERVICE_1_APIKEY", "my-api-key");
    env.put("SERVICE_1_CLIENT_ID", "my-client-id");
    env.put("SERVICE_1_CLIENT_SECRET", "my-client-secret");
    env.put("SERVICE_1_AUTH_URL", "https://iamhost/iam/api");
    env.put("SERVICE_1_AUTH_DISABLE_SSL", "true");
    env.put("SERVICE2_AUTH_TYPE", Authenticator.AUTHTYPE_BASIC);
    env.put("SERVICE2_USERNAME", "my-user");
    env.put("SERVICE2_PASSWORD", "my-password");
    env.put("SERVICE3_AUTH_TYPE", "Cp4D");
    env.put("SERVICE3_AUTH_URL", "https://cp4dhost/cp4d/api");
    env.put("SERVICE3_USERNAME", "my-cp4d-user");
    env.put("SERVICE3_PASSWORD", "my-cp4d-password");
    env.put("SERVICE3_AUTH_DISABLE_SSL", "false");
    env.put("SERVICE4_AUTH_TYPE", Authenticator.AUTHTYPE_NOAUTH);
    env.put("SERVICE5_AUTH_TYPE", Authenticator.AUTHTYPE_BEARER_TOKEN);
    env.put("SERVICE5_BEARER_TOKEN", "my-bearer-token");
    env.put("  SERVICE6_URL", "  https://service6/api  ");
    env.put("  SERVICE6_BEARER_TOKEN", "  my-bearer-token  ");
    env.put("SERVICE_7_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    env.put("SERVICE_7_APIKEY", "V4HXmoUtMjohnsnow=KotN");
    env.put("SERVICE_7_CLIENT_ID", "somefake========id");
    env.put("SERVICE_7_CLIENT_SECRET", "==my-client-secret==");
    env.put("SERVICE_7_AUTH_URL", "https://iamhost/iam/api=");
    env.put("SERVICE_8_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    env.put("SERVICE_8_APIKEY", "V4HXmoUtMjohnsnow=KotN");
    env.put("SERVICE_8_SCOPE", "A B C D");
    env.put("SERVICE_9_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    env.put("SERVICE_9_APIKEY", "my-api-key");
    env.put("SERVICE_9_CLIENT_ID", "my-client-id");
    env.put("SERVICE_9_CLIENT_SECRET", "my-client-secret");
    env.put("SERVICE_9_AUTH_URL", "https://iamhost/iam/api");
    env.put("SERVICE_9_ENABLE_GZIP", "true");

    return env;
  }

  private void setTestSystemProps() {
    System.setProperty("SERVICE_1_URL", "https://service1/api");
    System.setProperty("SERVICE_1_DISABLE_SSL", "true");
    System.setProperty("SERVICE2_URL", "https://service2/api");
    System.setProperty("SERVICE2_DISABLE_SSL", "false");
    System.setProperty("SERVICE3_URL", "https://service3/api");
    System.setProperty("SERVICE3_DISABLE_SSL", "false");
    System.setProperty("SERVICE4_URL", "https://service4/api");
    System.setProperty("SERVICE4_DISABLE_SSL", "false");
    System.setProperty("SERVICE5_URL", "https://service5/api");
    System.setProperty("SERVICE5_DISABLE_SSL", "true");
    System.setProperty("SERVICE_1_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    System.setProperty("SERVICE_1_APIKEY", "my-api-key");
    System.setProperty("SERVICE_1_CLIENT_ID", "my-client-id");
    System.setProperty("SERVICE_1_CLIENT_SECRET", "my-client-secret");
    System.setProperty("SERVICE_1_AUTH_URL", "https://iamhost/iam/api");
    System.setProperty("SERVICE_1_AUTH_DISABLE_SSL", "true");
    System.setProperty("SERVICE2_AUTH_TYPE", Authenticator.AUTHTYPE_BASIC);
    System.setProperty("SERVICE2_USERNAME", "my-user");
    System.setProperty("SERVICE2_PASSWORD", "my-password");
    System.setProperty("SERVICE3_AUTH_TYPE", "Cp4D");
    System.setProperty("SERVICE3_AUTH_URL", "https://cp4dhost/cp4d/api");
    System.setProperty("SERVICE3_USERNAME", "my-cp4d-user");
    System.setProperty("SERVICE3_PASSWORD", "my-cp4d-password");
    System.setProperty("SERVICE3_AUTH_DISABLE_SSL", "false");
    System.setProperty("SERVICE4_AUTH_TYPE", Authenticator.AUTHTYPE_NOAUTH);
    System.setProperty("SERVICE5_AUTH_TYPE", Authenticator.AUTHTYPE_BEARER_TOKEN);
    System.setProperty("SERVICE5_BEARER_TOKEN", "my-bearer-token");
    System.setProperty("SERVICE6_URL", "  https://service6/api  ");
    System.setProperty("SERVICE6_BEARER_TOKEN", "  my-bearer-token  ");
    System.setProperty("SERVICE_7_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    System.setProperty("SERVICE_7_APIKEY", "V4HXmoUtMjohnsnow=KotN");
    System.setProperty("SERVICE_7_CLIENT_ID", "somefake========id");
    System.setProperty("SERVICE_7_CLIENT_SECRET", "==my-client-secret==");
    System.setProperty("SERVICE_7_AUTH_URL", "https://iamhost/iam/api=");
    System.setProperty("SERVICE_8_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    System.setProperty("SERVICE_8_APIKEY", "V4HXmoUtMjohnsnow=KotN");
    System.setProperty("SERVICE_8_SCOPE", "A B C D");
    System.setProperty("SERVICE_9_AUTH_TYPE", Authenticator.AUTHTYPE_IAM);
    System.setProperty("SERVICE_9_APIKEY", "my-api-key");
    System.setProperty("SERVICE_9_CLIENT_ID", "my-client-id");
    System.setProperty("SERVICE_9_CLIENT_SECRET", "my-client-secret");
    System.setProperty("SERVICE_9_AUTH_URL", "https://iamhost/iam/api");
    System.setProperty("SERVICE_9_ENABLE_GZIP", "true");
  }

  private void clearTestSystemProps() {
    System.clearProperty("SERVICE_1_URL");
    System.clearProperty("SERVICE_1_DISABLE_SSL");
    System.clearProperty("SERVICE2_URL");
    System.clearProperty("SERVICE2_DISABLE_SSL");
    System.clearProperty("SERVICE3_URL");
    System.clearProperty("SERVICE3_DISABLE_SSL");
    System.clearProperty("SERVICE4_URL");
    System.clearProperty("SERVICE4_DISABLE_SSL");
    System.clearProperty("SERVICE5_URL");
    System.clearProperty("SERVICE5_DISABLE_SSL");
    System.clearProperty("SERVICE_1_AUTH_TYPE");
    System.clearProperty("SERVICE_1_APIKEY");
    System.clearProperty("SERVICE_1_CLIENT_ID");
    System.clearProperty("SERVICE_1_CLIENT_SECRET");
    System.clearProperty("SERVICE_1_AUTH_URL");
    System.clearProperty("SERVICE_1_AUTH_DISABLE_SSL");
    System.clearProperty("SERVICE2_AUTH_TYPE");
    System.clearProperty("SERVICE2_USERNAME");
    System.clearProperty("SERVICE2_PASSWORD");
    System.clearProperty("SERVICE3_AUTH_TYPE");
    System.clearProperty("SERVICE3_AUTH_URL");
    System.clearProperty("SERVICE3_USERNAME");
    System.clearProperty("SERVICE3_PASSWORD");
    System.clearProperty("SERVICE3_AUTH_DISABLE_SSL");
    System.clearProperty("SERVICE4_AUTH_TYPE");
    System.clearProperty("SERVICE5_AUTH_TYPE");
    System.clearProperty("SERVICE5_BEARER_TOKEN");
    System.clearProperty("SERVICE6_URL");
    System.clearProperty("SERVICE6_BEARER_TOKEN");
    System.clearProperty("SERVICE_7_AUTH_TYPE");
    System.clearProperty("SERVICE_7_APIKEY");
    System.clearProperty("SERVICE_7_CLIENT_ID");
    System.clearProperty("SERVICE_7_CLIENT_SECRET");
    System.clearProperty("SERVICE_7_AUTH_URL");
    System.clearProperty("SERVICE_8_AUTH_TYPE");
    System.clearProperty("SERVICE_8_APIKEY");
    System.clearProperty("SERVICE_8_SCOPE");
    System.clearProperty("SERVICE_9_AUTH_TYPE");
    System.clearProperty("SERVICE_9_APIKEY");
    System.clearProperty("SERVICE_9_CLIENT_ID");
    System.clearProperty("SERVICE_9_CLIENT_SECRET");
    System.clearProperty("SERVICE_9_AUTH_URL");
    System.clearProperty("SERVICE_9_ENABLE_GZIP");
  }

  /**
   * Setup.
   */
  public void setupVCAP() {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(VCAP_SERVICES);
    final String vcapServices = getStringFromInputStream(in);

    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("VCAP_SERVICES")).thenReturn(vcapServices);
  }

  @Test
  public void testGetVcapValueWithNullOrEmptyService() {
    setupVCAP();
    assertNull(CredentialUtils.getVcapServiceEntry(null));
    assertNull(CredentialUtils.getVcapServiceEntry(""));
  }

  @Test
  public void testGetVcapValueWithNoValuesSet() {
    setupVCAP();
    assertNull(CredentialUtils.getVcapServiceEntry("empty_service"));
  }

  @Test
  public void testGetVcapValueWithServiceName() {
    setupVCAP();
    assertNotNull(CredentialUtils.getVcapServiceEntry("discovery"));
  }

  @Test
  public void testBadCredentialChar() {
    // valid
    assertFalse(CredentialUtils.hasBadStartOrEndChar("this_is_fine"));

    // starting bracket
    assertTrue(CredentialUtils.hasBadStartOrEndChar("{bad_username"));
    assertTrue(CredentialUtils.hasBadStartOrEndChar("{{still_bad"));

    // ending bracket
    assertTrue(CredentialUtils.hasBadStartOrEndChar("invalid}"));
    assertTrue(CredentialUtils.hasBadStartOrEndChar("also_invalid}}"));

    // starting quote
    assertTrue(CredentialUtils.hasBadStartOrEndChar("\"not_allowed_either"));
    assertTrue(CredentialUtils.hasBadStartOrEndChar("\"\"still_not"));

    // ending quote
    assertTrue(CredentialUtils.hasBadStartOrEndChar("nope\""));
    assertTrue(CredentialUtils.hasBadStartOrEndChar("sorry\"\""));
  }

  @Test
  public void testFileCredentialsMapEmpty() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(null);
    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-1");
    assertTrue(props.isEmpty());
  }

  @Test
  public void testFileCredentialsMapService1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-1");
    verifyMapService1(props);
  }

  @Test
  public void testFileCredentialsMapService2() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service2");
    verifyMapService2(props);
  }

  @Test
  public void testFileCredentialsMapService3() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service3");
    verifyMapService3(props);
  }

  @Test
  public void testFileCredentialsMapService4() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service4");
    verifyMapService4(props);
  }

  @Test
  public void testFileCredentialsMapService5() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service5");
    verifyMapService5(props);
  }

  @Test
  public void testFileCredentialsMapService6() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service6");
    verifyMapService6(props);
  }

  @Test
  public void testFileCredentialsMapService7() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-7");
    verifyMapService7(props);
  }

  @Test
  public void testFileCredentialsMapService8() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-8");
    verifyMapService8(props);
  }

  @Test
  public void testFileCredentialsMapService9() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-9");
    verifyMapService9(props);
  }

  @Test
  public void testFileCredentialsSystemPropEmpty() {
    System.setProperty("IBM_CREDENTIALS_FILE", "");
    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-1");
    assertTrue(props.isEmpty());
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService1() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-1");
    verifyMapService1(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService2() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service2");
    verifyMapService2(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService3() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service3");
    verifyMapService3(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService4() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service4");
    verifyMapService4(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService5() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service5");
    verifyMapService5(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService6() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service6");
    verifyMapService6(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService7() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-7");
    verifyMapService7(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService8() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-8");
    verifyMapService8(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testFileCredentialsSystemPropService9() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
    assertEquals(ALTERNATE_CRED_FILENAME, System.getProperty("IBM_CREDENTIALS_FILE"));

    Map<String, String> props = CredentialUtils.getFileCredentialsAsMap("service-9");
    verifyMapService9(props);
    System.clearProperty("IBM_CREDENTIALS_FILE");
    assertNull(System.getProperty("IBM_CREDENTIALS_FILE"));
  }

  @Test
  public void testEnvCredentialsMapEmpty() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(new HashMap<String, String>());
    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service-1");
    assertTrue(props.isEmpty());
  }

  @Test
  public void testEnvCredentialsMapService1() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service-1");
    verifyMapService1(props);
  }

  @Test
  public void testEnvCredentialsMapService2() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service2");
    verifyMapService2(props);
  }

  @Test
  public void testEnvCredentialsMapService3() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service3");
    verifyMapService3(props);
  }

  @Test
  public void testEnvCredentialsMapService4() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service4");
    verifyMapService4(props);
  }

  @Test
  public void testEnvCredentialsMapService5() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service5");
    verifyMapService5(props);
  }

  @Test
  public void testEnvCredentialsMapService6() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service6");
    verifyMapService6(props);
  }

  @Test
  public void testEnvCredentialsMapService7() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service-7");
    verifyMapService7(props);
  }

  @Test
  public void testEnvCredentialsMapService8() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service-8");
    verifyMapService8(props);
  }

  @Test
  public void testEnvCredentialsMapService9() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());

    Map<String, String> props = CredentialUtils.getEnvCredentialsAsMap("service-9");
    verifyMapService9(props);
  }

  @Test
  public void testSystemPropsCredentialsEmpty() {
    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service-1");
    assertTrue(props.isEmpty());

    // Setting Only environment variables should still result in empty props
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv()).thenReturn(getTestProcessEnvironment());
    props = CredentialUtils.getSystemPropsCredentialsAsMap("service-1");
    assertTrue(props.isEmpty());
  }

  @Test
  public void testSystemPropsCredentialsService1() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service-1");
    verifyMapService1(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService2() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service2");
    verifyMapService2(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService3() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service3");
    verifyMapService3(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService4() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service4");
    verifyMapService4(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService5() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service5");
    verifyMapService5(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService6() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service6");
    verifyMapService6(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService7() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service-7");
    verifyMapService7(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService8() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service-8");
    verifyMapService8(props);
    clearTestSystemProps();
  }

  @Test
  public void testSystemPropsCredentialsService9() {
    setTestSystemProps();

    Map<String, String> props = CredentialUtils.getSystemPropsCredentialsAsMap("service-9");
    verifyMapService9(props);
    clearTestSystemProps();
  }

  @Test
  public void testVcapCredentialsMapEmpty() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("NOT_A_SERVICE");
    assertNotNull(props);
    assertTrue(props.isEmpty());
  }

  @Test
  public void testVcapCredentialsMapBasicAuth() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("discovery");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals(NOT_A_USERNAME, props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals(NOT_A_PASSWORD, props.get(Authenticator.PROPNAME_PASSWORD));
    assertEquals("https://gateway.watsonplatform.net/discovery-experimental/api", props.get("URL"));
  }
  @Test
  public void testVcapCredentialsNoMatchingName() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("no_matching_name");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals(NOT_A_USERNAME, props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals(NOT_A_PASSWORD, props.get(Authenticator.PROPNAME_PASSWORD));
    assertEquals("https://gateway.watsonplatform.net/different-name-two/api", props.get("URL"));
  }

  @Test
  public void testVcapCredentialsDuplicateName() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("service_entry_key_and_key_to_service_entries");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals(NOT_A_USERNAME, props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals(NOT_A_PASSWORD, props.get(Authenticator.PROPNAME_PASSWORD));
    assertEquals("https://on.the.toolchainplatform.net/devops-insights/api", props.get("URL"));
  }

  @Test
  public void testVcapCredentialsMissingNameField() {
    setupVCAP();
    final String username = "not-a-username-3";
    final String password = "not-a-password-3";

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("key_to_service_entry_2");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals(username, props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals(password, props.get(Authenticator.PROPNAME_PASSWORD));
    assertEquals("https://on.the.toolchainplatform.net/devops-insights-3/api", props.get("URL"));
  }

  @Test
  public void testVcapCredentialsEntryNotFound() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("fake_entry");
    assertNotNull(props);
    assertTrue(props.isEmpty());
    assertNull(props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_USERNAME));
    assertNull(props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get("URL"));
  }

  @Test
  public void testVcapCredentialsVcapNotSet() {
    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("fake_entry");
    assertNotNull(props);
    assertTrue(props.isEmpty());
    assertNull(props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_USERNAME));
    assertNull(props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get("URL"));
  }

  @Test
  public void testVcapCredentialsEmptySvcName() {
    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("");
    assertNotNull(props);
    assertTrue(props.isEmpty());
    assertNull(props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_USERNAME));
    assertNull(props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get("URL"));
  }

  @Test
  public void testVcapCredentialsNullSvcName() {
    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap(null);
    assertNotNull(props);
    assertTrue(props.isEmpty());
    assertNull(props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_USERNAME));
    assertNull(props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get("URL"));
  }

  @Test
  public void testVcapCredentialsNoCreds() {
    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("no-creds-service-two");
    assertNotNull(props);
    assertTrue(props.isEmpty());
    assertNull(props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_USERNAME));
    assertNull(props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get("URL"));
  }

  @Test
  public void testVcapCredentialsMapIAM() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("language_translator");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("123456789", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("https://iam.cloud.ibm.com/identity/token", props.get(Authenticator.PROPNAME_URL));
    assertEquals("https://gateway.watsonplatform.net/language-translator/api", props.get("URL"));
  }

  @Test
  public void testVcapCredentialsWhitespace() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("whitespace");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals("https://johnsnow/url/api/", props.get("URL"));
  }

  @Test
  public void testVcapCredentialsEqualsSign() {
    setupVCAP();

    Map<String, String> props = CredentialUtils.getVcapCredentialsAsMap("equals-sign-test");
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("V4HXmoUtMjohnsnow=KotN", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("https://iamhost/iam/api=", props.get(Authenticator.PROPNAME_URL));
    assertEquals("https://gateway.watsonplatform.net/testService", props.get("URL"));
  }

  private void verifyMapService1(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-api-key", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("my-client-secret", props.get(Authenticator.PROPNAME_CLIENT_SECRET));
    assertEquals("my-client-id", props.get(Authenticator.PROPNAME_CLIENT_ID));
    assertEquals("https://iamhost/iam/api", props.get(Authenticator.PROPNAME_URL));
    assertEquals("true", props.get(Authenticator.PROPNAME_DISABLE_SSL));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService2(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-user", props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals("my-password", props.get(Authenticator.PROPNAME_PASSWORD));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService3(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals("Cp4D", props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-cp4d-user", props.get(CloudPakForDataAuthenticator.PROPNAME_USERNAME));
    assertEquals("my-cp4d-password", props.get(CloudPakForDataAuthenticator.PROPNAME_PASSWORD));
    assertEquals("https://cp4dhost/cp4d/api", props.get(CloudPakForDataAuthenticator.PROPNAME_URL));
    assertEquals("false", props.get(CloudPakForDataAuthenticator.PROPNAME_DISABLE_SSL));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService4(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_NOAUTH, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService5(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-bearer-token", props.get(Authenticator.PROPNAME_BEARER_TOKEN));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService6(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals("https://service6/api", props.get("URL"));
    assertEquals("my-bearer-token", props.get(Authenticator.PROPNAME_BEARER_TOKEN));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService7(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("V4HXmoUtMjohnsnow=KotN", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("==my-client-secret==", props.get(Authenticator.PROPNAME_CLIENT_SECRET));
    assertEquals("somefake========id", props.get(Authenticator.PROPNAME_CLIENT_ID));
    assertEquals("https://iamhost/iam/api=", props.get(Authenticator.PROPNAME_URL));
    assertNull(props.get(Authenticator.PROPNAME_DISABLE_SSL));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
  }

  private void verifyMapService8(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("V4HXmoUtMjohnsnow=KotN", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("A B C D", props.get(Authenticator.PROPNAME_SCOPE));
    assertNull(props.get(Authenticator.PROPNAME_DISABLE_SSL));
  }

  private void verifyMapService9(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-api-key", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("my-client-secret", props.get(Authenticator.PROPNAME_CLIENT_SECRET));
    assertEquals("my-client-id", props.get(Authenticator.PROPNAME_CLIENT_ID));
    assertEquals("https://iamhost/iam/api", props.get(Authenticator.PROPNAME_URL));
    assertNull(props.get(Authenticator.PROPNAME_SCOPE));
    assertEquals("true", props.get(BaseService.PROPNAME_ENABLE_GZIP));
  }
}
