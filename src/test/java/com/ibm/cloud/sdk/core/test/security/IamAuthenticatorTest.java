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
import com.ibm.cloud.sdk.core.security.AuthenticatorBase;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.security.TokenRequestBasedAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings("deprecation")
public class IamAuthenticatorTest extends BaseServiceUnitTest {

  private IamToken tokenData;
  private IamToken refreshedTokenData;
  private String url;

  private static final String API_KEY = "123456789";

  // Logging level used by this test.
  // For debugging, set this to Level.FINE or Level.ALL, etc.
  private static Level logLevel = Level.SEVERE;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData = loadFixture("src/test/resources/iam_token.json", IamToken.class);
    refreshedTokenData = loadFixture("src/test/resources/refreshed_iam_token.json", IamToken.class);

    // Set up java.util.logging to display messages on the console.
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(logLevel);
    Logger logger;
    logger = Logger.getLogger(IamAuthenticator.class.getName());
    logger.setLevel(logLevel);
    logger.addHandler(handler);

    logger = Logger.getLogger(TokenRequestBasedAuthenticator.class.getName());
    logger.setLevel(logLevel);
    logger.addHandler(handler);
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
    new IamAuthenticator.Builder()
      .apikey(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyApiKey() {
    new IamAuthenticator.Builder()
      .apikey("")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInvalidApiKey() {
    new IamAuthenticator.Builder()
      .apikey("{apikey}")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingClientId() {
    new IamAuthenticator.Builder()
      .apikey(API_KEY)
      .clientId(null)
      .clientSecret("clientSecret")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyClientId() {
    new IamAuthenticator.Builder()
      .apikey(API_KEY)
      .clientId("")
      .clientSecret("clientSecret")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingClientSecret() {
    new IamAuthenticator.Builder()
      .apikey(API_KEY)
      .clientId("clientId")
      .clientSecret(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyClientSecret() {
    new IamAuthenticator.Builder()
      .apikey(API_KEY)
      .clientId("clientId")
      .clientSecret("")
      .build();
  }

  @Test
  public void testBuilderCorrectConfig() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url("url")
        .clientId("clientId")
        .clientSecret("clientSecret")
        .scope("scope1")
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .proxy(null)
        .proxyAuthenticator(null)
        .build();
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals("scope1", authenticator.getScope());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    IamAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);

    auth2 = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(null)
        .build();
    assertNotNull(auth2.getURL());
    auth2.setBasicAuthInfo("user", "pw");
    assertEquals("user", auth2.getClientId());
    assertEquals("user", auth2.getUsername());
    assertEquals("pw", auth2.getPassword());
    assertEquals("pw", auth2.getClientSecret());
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, "url");
    props.put(Authenticator.PROPNAME_CLIENT_ID, "clientId");
    props.put(Authenticator.PROPNAME_CLIENT_SECRET, "clientSecret");
    props.put(Authenticator.PROPNAME_SCOPE, "scope1 scope2");
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    IamAuthenticator authenticator = IamAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals("scope1 scope2", authenticator.getScope());
    assertTrue(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_SCOPE, "scope1");

    IamAuthenticator authenticator = IamAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("scope1", authenticator.getScope());
    assertNotNull(authenticator.getURL());
    assertNull(authenticator.getClientId());
    assertNull(authenticator.getClientSecret());
    assertFalse(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testDisableSSLVerification() {
    IamAuthenticator auth = new IamAuthenticator.Builder().apikey(API_KEY).build();
    assertFalse(auth.getDisableSSLVerification());
    auth.setDisableSSLVerification(true);
    assertTrue(auth.getDisableSSLVerification());

    auth = new IamAuthenticator.Builder().apikey(API_KEY).url("url").disableSSLVerification(true).build();
    assertTrue(auth.getDisableSSLVerification());
  }

  @Test
  public void testSetScope() {
    IamAuthenticator auth = new IamAuthenticator.Builder().apikey(API_KEY).build();
    assertNull(auth.getScope());
    String scope = "scope1 scope2 scope3";
    auth.setScope(scope);
    assertEquals(scope, auth.getScope());

    auth = auth.newBuilder()
        .apikey(API_KEY)
        .scope(null)
        .build();
    assertNull(auth.getScope());

    auth = auth.newBuilder()
        .scope(scope)
        .build();
    assertEquals (scope, auth.getScope());
  }


  //
  // Tests involving interactions with a mocked token service.
  //

  @Test
  public void testAuthenticateNewAndStoredToken() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
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
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now make sure the token server request did not contain an Authorization header,
    // since we didn't set clientId/clientSecret.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNull(actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Set mock time to be within tokenData's lifetime, but before the refresh/expiration times.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(tokenData.getExpiration() - 1000L);

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the expired token
    // set above in the mock server.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now set the mock time to reflect that the first access token ("tokenData") has expired.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(tokenData.getExpiration());

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    server.enqueue(jsonResponse(refreshedTokenData));
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getAccessToken());
  }

  @Test
  public void testAuthenticationExpiredToken10SecWindow() {
    server.enqueue(jsonResponse(tokenData));

    // Set initial mock time to be epoch time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the expired token
    // set above in the mock server.
    authenticator.authenticate(requestBuilder);

    // Now set the mock time to reflect that the first access token ("tokenData") is considered to be "expired".
    // We subtract 10s from the expiration time to test the boundary condition of the expiration window feature.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(
        tokenData.getExpiration() - IamToken.IamExpirationWindow);

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    server.enqueue(jsonResponse(refreshedTokenData));
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getAccessToken());
  }

  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    server.enqueue(jsonResponse(tokenData));

    // Set initial mock time to be epoch time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // This will bootstrap the test by forcing the Authenticator to store the first access token (tokenData).
    authenticator.authenticate(requestBuilder);

    // Now set the mock time to put us in the "refresh window" where the token is not expired,
    // but still needs to be refreshed.
    long refreshWindow = (long) (tokenData.getExpiresIn() * 0.2);
    long refreshTime = tokenData.getExpiration() - refreshWindow;
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(refreshTime + 2L);

    // Authenticator should detect the need to refresh and request a new access token IN THE BACKGROUND when we call
    // authenticate() again. The immediate response should be the token which was already stored, since it's not yet
    // expired.
    server.enqueue(jsonResponse(refreshedTokenData).setBodyDelay(2, TimeUnit.SECONDS));
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Next request should use the refreshed token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getAccessToken());
  }

  @Test
  public void testUserHeaders() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    headers.put("Host", "iam.cloud.ibm.com:81");
    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .headers(headers)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals("value1", actualHeaders.get("header1"));
    assertEquals("value2", actualHeaders.get("header2"));
    assertEquals("iam.cloud.ibm.com:81", actualHeaders.get("Host"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());
  }

  @Test
  public void testClientIdSecret() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .clientId("clientId")
        .clientSecret("clientSecret")
        .build();
    String expectedIAMAuthHeader = AuthenticatorBase.constructBasicAuthHeader("clientId", "clientSecret");

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now make sure the token server request contained the correct Authorization header.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals(expectedIAMAuthHeader, actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());
  }

  @Test
  public void testFormBodyParams() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .build();
    assertNull(authenticator.getScope());
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    String body = tokenServerRequest.getBody().readUtf8();
    String expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=123456789&response_type=cloud_iam";
    assertEquals(expectedBody, body);
  }

  @Test
  public void testFormBodyParamsWScope() throws Throwable {
    String scope = "scope1 scope2 scope3";

    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
        .apikey(API_KEY)
        .url(url)
        .scope(scope)
        .build();
    assertEquals(scope, authenticator.getScope());
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getAccessToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    String body = tokenServerRequest.getBody().readUtf8();
    String expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=123456789&response_type=cloud_iam&scope=scope1%20scope2%20scope3";
    assertEquals(expectedBody, body);
  }

  // @Ignore
  @Test(expectedExceptions = ServiceResponseException.class)
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
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

    IamAuthenticator authenticator = new IamAuthenticator.Builder()
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


  //
  // Tests involving the deprecated ctors.
  //

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingApiKey() {
    new IamAuthenticator((String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyApiKey() {
    new IamAuthenticator("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingApiKeyMap() {
    Map<String, String> props = new HashMap<>();
    new IamAuthenticator(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingClientId() {
    new IamAuthenticator(API_KEY, "url", null, "clientSecret", false, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyClientId() {
    new IamAuthenticator(API_KEY, "url", "", "clientSecret", false, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingClientSecret() {
    new IamAuthenticator(API_KEY, "url", "clientId", null, false, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyClientSecret() {
    new IamAuthenticator(API_KEY, "url", "clientId", "", false, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyApiKeyMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, "");
    new IamAuthenticator(props);
  }

  @Test
  public void testCtorCorrectConfig() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, "url", "clientId", "clientSecret", true, expectedHeaders, "scope1");
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals("scope1", authenticator.getScope());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
  }

  @Test
  public void testCtorCorrectConfigMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, "url");
    props.put(Authenticator.PROPNAME_CLIENT_ID, "clientId");
    props.put(Authenticator.PROPNAME_CLIENT_SECRET, "clientSecret");
    props.put(Authenticator.PROPNAME_SCOPE, "scope1 scope2");
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    IamAuthenticator authenticator = new IamAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals("scope1 scope2", authenticator.getScope());
    assertTrue(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testCorrectConfigMap2() {
    Map<String, String> props = new HashMap<>();
    props.put("IAM_APIKEY", API_KEY);

    IamAuthenticator authenticator = new IamAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertNotNull(authenticator.getURL());
    assertNull(authenticator.getClientId());
    assertNull(authenticator.getClientSecret());
    assertNull(authenticator.getScope());
    assertFalse(authenticator.getDisableSSLVerification());
  }
}
