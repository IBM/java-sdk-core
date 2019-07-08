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

import com.ibm.cloud.sdk.core.util.CredentialUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Hashtable;

import static com.ibm.cloud.sdk.core.test.TestUtils.getStringFromInputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Class CredentialUtilsTest.
 */
public class CredentialUtilsTest {
  private static final String SERVICE_NAME = "personality_insights";
  private static final String VCAP_SERVICES = "vcap_services.json";
  private static final String APIKEY = "apikey";
  private static final String USERNAME = "username";
  private static final String OLD_API_KEY = "api_key";
  private static final String NOT_A_USERNAME = "not-a-username";
  private static final String NOT_A_PASSWORD = "not-a-password";
  private static final String NOT_A_FREE_USERNAME = "not-a-free-username";
  private static final String VISUAL_RECOGNITION = "watson_vision_combined";
  private static final String NLC_SERVICE_NAME = "natural_language_classifier";
  private static final String TEST_APIKEY = "123456789";
  private static final String NLC_URL = "https://gateway.watsonplatform.net/natural-language-classifier/api";
  private static final String ASSISTANT_SERVICE_NAME = "assistant";
  private static final String TEST_ACCESS_TOKEN =
      "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFkbWluIiwicGVybWlzc2lvbnMiOlsiYWRtaW5pc3RyYXRvciIsIm1hbmFnZV9jYXRhbG9nIiwiYWNjZXNzX2NhdGFsb2ciLCJtYW5hZ2VfcG9saWNpZXMiLCJhY2Nlc3NfcG9saWNpZXMiLCJ2aXJ0dWFsaXplX3RyYW5zZm9ybSIsImNhbl9wcm92aXNpb24iLCJkZXBsb3ltZW50X2FkbWluIl0sInN1YiI6ImFkbWluIiwiaXNzIjoiS05PWFNTTyIsImF1ZCI6IkRTWCIsInVpZCI6Ijk5OSIsImlhdCI6MTU1OTMyODk1NSwiZXhwIjo5OTk5OTk5OTk5OTk5OTk5OTl9.GE-ML3JWmI3oB0z5mjMG3jFtYVVA5bQCsOTOOj9ab7PcgJc1mA5hn1sONkO0JAFADhUoAgpG4KgQef5tjnCSrtl1tbnDuhaP1DH4QKMCZOkWrKyfQ2X8P1jhyJmV-KpE4wuTrGdMoMVj4wVRZwnxMRSK6LhV6pIzyOLLYR21zcW_2KcKWxCYfIC7tiM1d2PSM5nWa_5Vb068F8PtdiFUElEYHYKrvmwpV57_k2jpXoY6zw8PDcIiWQe3g20w6vCB6zWhxbcFWyjMg1tPOZHgTNNskPShHQBbtZFsSrc7rkNPzttKF70m7_JqrRYUZDNN8TmuR9uyitwxEFkr2L0WDQ";
  private static final String TEST_ICP_INSTANCE_URL = "https://icp" +
      ".cluster:31843/assistant/test/instances/1560453115/api";

  /**
   * Setup.
   */
  @Before
  public void setup() {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(VCAP_SERVICES);
    final String vcapServices = getStringFromInputStream(in);
    CredentialUtils.setServices(vcapServices);

    final Hashtable<String, String> env = new Hashtable<>();
    env.put("java.naming.factory.initial", "org.osjava.sj.SimpleContextFactory");
    env.put("org.osjava.sj.delimiter", "/");
    env.put("org.osjava.sj.root", "src/test/resources");

    CredentialUtils.setContext(env);
  }

  @Test
  public void testGetVcapValueWithNullOrEmptyService() {
    assertNull(CredentialUtils.getVcapValue(null, APIKEY));
    assertNull(CredentialUtils.getVcapValue("", APIKEY));
  }

  @Test
  public void testGetVcapValueWithPlan() {
    assertEquals(NOT_A_USERNAME, CredentialUtils.getVcapValue(SERVICE_NAME, USERNAME, CredentialUtils.PLAN_STANDARD));
  }

  @Test
  public void testGetVcapValueWithoutPlan() {
    assertEquals(NOT_A_PASSWORD, CredentialUtils.getVcapValue(VISUAL_RECOGNITION, OLD_API_KEY));
  }

  @Test
  public void testGetVcapValueWithMultiplePlans() {
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
  public void testGetFileCredentials() {
    CredentialUtils.ServiceCredentials testCredentials
        = CredentialUtils.getFileCredentials(NLC_SERVICE_NAME);

    assertEquals(TEST_APIKEY, testCredentials.getIamApiKey());
    assertEquals(NLC_URL, testCredentials.getUrl());
  }

  @Test
  public void testGetFileCredentialsWithMissingService() {
    CredentialUtils.ServiceCredentials emptyCredentials
        = CredentialUtils.getFileCredentials(VISUAL_RECOGNITION);

    assertNull(emptyCredentials.getUsername());
    assertNull(emptyCredentials.getPassword());
    assertNull(emptyCredentials.getOldApiKey());
    assertNull(emptyCredentials.getUrl());
    assertNull(emptyCredentials.getIamApiKey());
    assertNull(emptyCredentials.getIamUrl());
  }

  @Test
  public void testGetFileCredentialsIcp4d() {
    CredentialUtils.ServiceCredentials testCredentials
        = CredentialUtils.getFileCredentials(ASSISTANT_SERVICE_NAME);

    assertEquals(TEST_ACCESS_TOKEN, testCredentials.getAccessToken());
    assertEquals(TEST_ICP_INSTANCE_URL, testCredentials.getUrl());
  }
}
