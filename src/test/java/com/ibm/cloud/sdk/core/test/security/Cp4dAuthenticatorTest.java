/**
 * (C) Copyright IBM Corp. 2019, 2021.
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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataAuthenticator;
import com.ibm.cloud.sdk.core.security.Cp4dTokenResponse;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Clock.class })
@PowerMockIgnore("javax.net.ssl.*")
@SuppressWarnings("deprecation")
public class Cp4dAuthenticatorTest extends BaseServiceUnitTest {

  // Token with issued-at time of 1574353085 and expiration time of 1574453956
  private Cp4dTokenResponse tokenData;

  // Token with issued-at time of 1602788645 and expiration time of 1999999999
  private Cp4dTokenResponse refreshedTokenData;

  private String url;
  private String testUsername = "test-username";
  private String testPassword = "test-password";
  private String testApikey = "test-apikey";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData = loadFixture("src/test/resources/cp4d_token.json", Cp4dTokenResponse.class);
    refreshedTokenData = loadFixture("src/test/resources/refreshed_cp4d_token.json", Cp4dTokenResponse.class);
  }


  //
  // Tests involving the new Builder class and fromConfiguration() method.
  //

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderMissingURL() {
    new CloudPakForDataAuthenticator.Builder()
      .url(null)
      .username(testUsername)
      .password(testPassword)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEmptyURL() {
    new CloudPakForDataAuthenticator.Builder()
      .url("")
      .username(testUsername)
      .password(testPassword)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderMissingUsername() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username(null)
      .password(testPassword)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEmptyUsername() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username("")
      .password(testPassword)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderMissingPassword() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .password(null)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEmptyPassword() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .password("")
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderMissingApikey() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey(null)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEmptyApikey() {
    new CloudPakForDataAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey("")
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigMissingUrl() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, null);
    props.put(Authenticator.PROPNAME_USERNAME, "testUsername");
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    CloudPakForDataAuthenticator.fromConfiguration(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigMissingUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, null);
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    CloudPakForDataAuthenticator.fromConfiguration(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigMissingPassword() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_PASSWORD, null);
    CloudPakForDataAuthenticator.fromConfiguration(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigMissingApikey() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, null);
    CloudPakForDataAuthenticator.fromConfiguration(props);
  }

  @Test
  public void testBuilderCorrectConfig1() {
    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testPassword, authenticator.getPassword());
    assertNull(authenticator.getApikey());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig2() {
    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertNull(authenticator.getPassword());
    assertEquals(testApikey, authenticator.getApikey());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig3() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertNull(authenticator.getPassword());
    assertEquals(testApikey, authenticator.getApikey());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig1() {
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
    assertNull(authenticator.getApikey());
    assertTrue(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertNull(authenticator.getPassword());
    assertEquals(testApikey, authenticator.getApikey());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }


  //
  // Tests involving interactions with a mocked token service.
  //

  @Test
  public void testAuthenticateNewAndStoredToken() {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we're way before the token expiration time.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .disableSSLVerification(true)
        .build();
    Request.Builder requestBuilder;

    // Authenticator should request new, valid token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we've passed the token expiration time.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 1800000000);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .build();
    Request.Builder requestBuilder;

    // This will bootstrap the test by forcing the Authenticator to store the expired token
    // set above in the mock server.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    server.enqueue(jsonResponse(refreshedTokenData));

    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getToken());
  }

  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to put us in the "refresh window" where the token is not expired but still needs refreshed.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 1574453700);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .disableSSLVerification(true)
        .build();
    Request.Builder requestBuilder;

    // This will bootstrap the test by forcing the Authenticator to store the token needing refreshed, which was
    // set above in the mock server.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);

    // Authenticator should detect the need to refresh and request a new access token IN THE BACKGROUND when we call
    // authenticate() again. The immediate response should be the token which was already stored, since it's not yet
    // expired.
    server.enqueue(jsonResponse(refreshedTokenData).setBodyDelay(2, TimeUnit.SECONDS));

    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Next request should use the refreshed token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getToken());
  }

  @Test
  public void testUserHeaders() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .disableSSLVerification(true)
        .build();

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    authenticator.setHeaders(headers);

    Request.Builder requestBuilder;

    // Authenticator should request new, valid token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals("value1", actualHeaders.get("header1"));
    assertEquals("value2", actualHeaders.get("header2"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());
  }

  @Test
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    try {
      authenticator.authenticate(requestBuilder);
      fail("Expected authenticate() to result in exception!");
    } catch (ServiceResponseException excp) {
      assertTrue(excp instanceof ServiceResponseException);
    } catch (Throwable t) {
      fail("Expected ServiceResponseException, not " + t.getClass().getSimpleName());
    }
  }

  @Test
  public void testApiErrorResponse() throws Throwable {
    server.enqueue(jsonResponse("{'}"));

    // Mock current time to ensure the token is valid.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    try {
      authenticator.authenticate(requestBuilder);
      fail("Expected authenticate() to result in exception!");
    } catch (RuntimeException excp) {
      Throwable causedBy = excp.getCause();
      assertNotNull(causedBy);
      assertTrue(causedBy instanceof IllegalStateException);
    } catch (Throwable t) {
      fail("Expected RuntimeException, not " + t.getClass().getSimpleName());
    }
  }

  // Verify the Authorization header in the specified request builder.
  private void verifyAuthHeader(Request.Builder builder, String expectedPrefix) {
    Request request = builder.build();
    String actualValue = request.header(HttpHeaders.AUTHORIZATION);
    assertNotNull(actualValue);

    assertTrue(actualValue.startsWith(expectedPrefix));
  }


  //
  // Tests involving the deprecated ctors.
  //

  @Test(expected = IllegalArgumentException.class)
  public void testCtorMissingURL() {
    new CloudPakForDataAuthenticator(null, testUsername, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorEmptyURL() {
    new CloudPakForDataAuthenticator("", testUsername, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorMissingUsername() {
    new CloudPakForDataAuthenticator("https://good-url", null, testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorEmptyUsername() {
    new CloudPakForDataAuthenticator("https://good-url", "", testPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorMissingPassword() {
    new CloudPakForDataAuthenticator("https://good-url", testUsername, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorEmptyPassword() {
    new CloudPakForDataAuthenticator("https://good-url", testUsername, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorMissingUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    new CloudPakForDataAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorEmptyUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_PASSWORD, testPassword);
    props.put(Authenticator.PROPNAME_USERNAME, "");
    new CloudPakForDataAuthenticator(props);
  }

  @Test
  public void testCtorCorrectConfig() {
    CloudPakForDataAuthenticator authenticator = new CloudPakForDataAuthenticator(url, testUsername, testPassword);
    assertEquals(Authenticator.AUTHTYPE_CP4D, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testPassword, authenticator.getPassword());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testCtorCorrectConfigMap() {
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
}
