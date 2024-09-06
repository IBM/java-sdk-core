/**
 * (C) Copyright IBM Corp. 2023, 2024.
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.MCSPAuthenticator;
import com.ibm.cloud.sdk.core.security.MCSPTokenResponse;
import com.ibm.cloud.sdk.core.security.TokenRequestBasedAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class MCSPAuthenticatorTest extends BaseServiceUnitTest {

  // Token with issued-at time of 1699026536 and expiration time of 1699033736
  private MCSPTokenResponse tokenData;

  // Token with issued-at time of 1699037852 and expiration time of 1699045052
  private MCSPTokenResponse refreshedTokenData;

  // The mock server's URL.
  private String url;

  private static final String API_KEY = "123456789";
  private static final String AUTH_URL = "https://mcsp.token-exchange.com";

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData = loadFixture("src/test/resources/mcsp_token.json", MCSPTokenResponse.class);
    refreshedTokenData = loadFixture("src/test/resources/refreshed_mcsp_token.json", MCSPTokenResponse.class);
  }

  // This will be our mocked version of the Clock class.
  private static MockedStatic<Clock> clockMock = null;

  @BeforeMethod
  public void createEnvMock() {
    clockMock = Mockito.mockStatic(Clock.class);
  }

  @AfterMethod
  public void clearEnvMock() {
    if (clockMock != null) {
      clockMock.close();
      clockMock = null;
    }
  }

  //
  // Tests involving the Builder class and fromConfiguration() method.
  //

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingApiKey() {
    new MCSPAuthenticator.Builder()
      .apikey(null)
      .url(AUTH_URL)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyApiKey() {
    new MCSPAuthenticator.Builder()
      .apikey("")
      .url(AUTH_URL)
      .build();
  }

  @Test
  public void testBuilderCorrectConfig() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
        .apikey(API_KEY)
        .url(AUTH_URL)
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .proxy(null)
        .proxyAuthenticator(null)
        .build();
    assertEquals(Authenticator.AUTHTYPE_MCSP, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals(AUTH_URL, authenticator.getURL());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    MCSPAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);
    assertEquals(Authenticator.AUTHTYPE_MCSP, auth2.authenticationType());
    assertEquals(API_KEY, auth2.getApiKey());
    assertEquals(AUTH_URL, auth2.getURL());
    assertTrue(auth2.getDisableSSLVerification());
    assertEquals(expectedHeaders, auth2.getHeaders());
    assertNull(auth2.getProxy());
    assertNull(auth2.getProxyAuthenticator());
  }

  @Test
  public void testConfigCorrectConfig() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, AUTH_URL);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    MCSPAuthenticator authenticator = MCSPAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_MCSP, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals(AUTH_URL, authenticator.getURL());
    assertTrue(authenticator.getDisableSSLVerification());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigIncorrectConfig() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, "");

    MCSPAuthenticator.fromConfiguration(props);
  }

  @Test
  public void testDisableSSLVerification() {
    MCSPAuthenticator auth = new MCSPAuthenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .build();
    assertFalse(auth.getDisableSSLVerification());
    auth.setDisableSSLVerification(true);
    assertTrue(auth.getDisableSSLVerification());

    auth = new MCSPAuthenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL).disableSSLVerification(true)
            .build();
    assertTrue(auth.getDisableSSLVerification());
  }


  //
  // Tests involving interactions with a mocked token service.
  //

  @Test
  public void testAuthenticateNewAndStoredToken() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we're way before the token expiration time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .disableSSLVerification(true)
        .build();

    // Create a custom client and set it on the authenticator.
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .allEnabledCipherSuites()
        .build();
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
        .build();
    authenticator.setClient(client);
    assertEquals(authenticator.getClient(), client);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we've passed the token expiration time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 1800000000);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the expired token
    // set above in the mock server.
    authenticator.authenticate(requestBuilder);

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    server.enqueue(jsonResponse(refreshedTokenData));
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getToken());
  }

  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to put us in the "refresh window" where the token is not expired but still needs refreshed.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 1699032300);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the token needing to be
    // refreshed, which was set above in the mock server.
    authenticator.authenticate(requestBuilder);

    // Authenticator should detect the need to refresh and request a new access token IN THE BACKGROUND when we call
    // authenticate() again. The immediate response should be the token which was already stored, since it's not yet
    // expired.
    server.enqueue(jsonResponse(refreshedTokenData).setBodyDelay(2, TimeUnit.SECONDS));
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
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    headers.put("Host", "mcsp.cloud.ibm.com:81");
    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .headers(headers)
            .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals("value1", actualHeaders.get("header1"));
    assertEquals("value2", actualHeaders.get("header2"));
    assertEquals("mcsp.cloud.ibm.com:81", actualHeaders.get("Host"));
    assertTrue(actualHeaders.get(HttpHeaders.USER_AGENT).startsWith("ibm-java-sdk-core/mcsp-authenticator"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());
  }

  @Test
  public void testRequestBody() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    String body = tokenServerRequest.getBody().readUtf8();
    String expectedBody = String.format("{\"apikey\":\"%s\"}", API_KEY);
    assertEquals(expectedBody, body);
  }

  // @Ignore
  @Test(expectedExceptions = ServiceResponseException.class)
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    authenticator.authenticate(requestBuilder);
  }

  @Test
  public void testApiResponseError() throws Throwable {
    server.enqueue(jsonResponse("{'}"));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    MCSPAuthenticator authenticator = new MCSPAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
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
}
