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

package com.ibm.cloud.sdk.core.test.security;

import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.Cp4dTokenResponse;
import com.ibm.cloud.sdk.core.security.CloudPakForDataAuthenticator;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class Cp4dAuthenticatorTest extends BaseServiceUnitTest {

  private Cp4dTokenResponse validTokenData;
  private Cp4dTokenResponse expiredTokenData;
  private String url;
  private String testUsername = "test-username";
  private String testPassword = "test-password";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    validTokenData = loadFixture("src/test/resources/valid_icp4d_token.json", Cp4dTokenResponse.class);
    expiredTokenData = loadFixture("src/test/resources/expired_icp4d_token.json", Cp4dTokenResponse.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingURL() {
    new CloudPakForDataAuthenticator(null, testUsername, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyURL() {
    new CloudPakForDataAuthenticator("", testUsername, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingUsername() {
    new CloudPakForDataAuthenticator("https://good-url", null, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUsername() {
    new CloudPakForDataAuthenticator("https://good-url", "", testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    new CloudPakForDataAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    props.put(Authenticator.PROPNAME_USERNAME, "");
    new CloudPakForDataAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingPassword() {
    new CloudPakForDataAuthenticator("https://good-url", testUsername, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyPassword() {
    new CloudPakForDataAuthenticator("https://good-url", testUsername, "");
  }

  @Test
  public void testCorrectConfig() {
    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(url, testUsername, testPassword);
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testPassword, authenticator.getPassword());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testCorrectConfigMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testPassword, authenticator.getPassword());
    assertTrue(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testAuthenticateNewAndStoredToken() {
    server.enqueue(jsonResponse(validTokenData));

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(url, testUsername, testPassword);
    authenticator.setDisableSSLVerification(true);
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);

    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));

    // Authenticator should just return the same token this time since we have a valid one stored.
    Request.Builder newBuilder = request.newBuilder();
    authenticator.authenticate(newBuilder);

    Request newRequest = newBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), newRequest.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(expiredTokenData));

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(url, testUsername, testPassword);
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the expired token
    // set above in the mock server.
    authenticator.authenticate(requestBuilder);

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    server.enqueue(jsonResponse(validTokenData));
    authenticator.authenticate(requestBuilder);

    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void testUserHeaders() throws Throwable {
    server.enqueue(jsonResponse(validTokenData));

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(url, testUsername, testPassword);
    authenticator.setDisableSSLVerification(true);

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    authenticator.setHeaders(headers);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);

    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals("value1", actualHeaders.get("header1"));
    assertEquals("value2", actualHeaders.get("header2"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    Request.Builder newBuilder = request.newBuilder();
    authenticator.authenticate(newBuilder);

    Request newRequest = newBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), newRequest.header(HttpHeaders.AUTHORIZATION));
  }
}
