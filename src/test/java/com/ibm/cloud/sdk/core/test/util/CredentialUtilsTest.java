/**
 * Copyright 2017 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.test.util;

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
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;

/**
 * The Class CredentialUtilsTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EnvironmentUtils.class })
public class CredentialUtilsTest {
  private static final String ALTERNATE_CRED_FILENAME = "src/test/resources/my-credentials.env";
  private static final String SERVICE_NAME = "personality_insights";
  private static final String VCAP_SERVICES = "vcap_services.json";
  private static final String APIKEY = "apikey";
  private static final String USERNAME = "username";
  private static final String OLD_API_KEY = "api_key";
  private static final String NOT_A_USERNAME = "not-a-username";
  private static final String NOT_A_PASSWORD = "not-a-password";
  private static final String NOT_A_FREE_USERNAME = "not-a-free-username";
  private static final String VISUAL_RECOGNITION = "watson_vision_combined";

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

    return env;
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
    assertNull(CredentialUtils.getVcapValue(null, APIKEY));
    assertNull(CredentialUtils.getVcapValue("", APIKEY));
  }

  @Test
  public void testGetVcapValueWithPlan() {
    setupVCAP();
    assertEquals(NOT_A_USERNAME, CredentialUtils.getVcapValue(SERVICE_NAME, USERNAME, CredentialUtils.PLAN_STANDARD));
  }

  @Test
  public void testGetVcapValueWithoutPlan() {
    setupVCAP();
    assertEquals(NOT_A_PASSWORD, CredentialUtils.getVcapValue(VISUAL_RECOGNITION, OLD_API_KEY));
  }

  @Test
  public void testGetVcapValueWithMultiplePlans() {
    setupVCAP();
    assertEquals(NOT_A_FREE_USERNAME, CredentialUtils.getVcapValue(SERVICE_NAME, USERNAME));
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

  private void verifyMapService1(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_IAM, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-api-key", props.get(Authenticator.PROPNAME_APIKEY));
    assertEquals("my-client-secret", props.get(Authenticator.PROPNAME_CLIENT_SECRET));
    assertEquals("my-client-id", props.get(Authenticator.PROPNAME_CLIENT_ID));
    assertEquals("https://iamhost/iam/api", props.get(Authenticator.PROPNAME_URL));
    assertEquals("true", props.get(Authenticator.PROPNAME_DISABLE_SSL));
  }

  private void verifyMapService2(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BASIC, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-user", props.get(Authenticator.PROPNAME_USERNAME));
    assertEquals("my-password", props.get(Authenticator.PROPNAME_PASSWORD));
  }

  private void verifyMapService3(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals("Cp4D", props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-cp4d-user", props.get(CloudPakForDataAuthenticator.PROPNAME_USERNAME));
    assertEquals("my-cp4d-password", props.get(CloudPakForDataAuthenticator.PROPNAME_PASSWORD));
    assertEquals("https://cp4dhost/cp4d/api", props.get(CloudPakForDataAuthenticator.PROPNAME_URL));
    assertEquals("false", props.get(CloudPakForDataAuthenticator.PROPNAME_DISABLE_SSL));
  }

  private void verifyMapService4(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_NOAUTH, props.get(Authenticator.PROPNAME_AUTH_TYPE));
  }

  private void verifyMapService5(Map<String, String> props) {
    assertNotNull(props);
    assertFalse(props.isEmpty());
    assertEquals(Authenticator.AUTHTYPE_BEARER_TOKEN, props.get(Authenticator.PROPNAME_AUTH_TYPE));
    assertEquals("my-bearer-token", props.get(Authenticator.PROPNAME_BEARER_TOKEN));
  }
}
