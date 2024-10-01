/**
 * (C) Copyright IBM Corp. 2024.
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

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAssumeAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class IamAssumeAuthenticatorTest extends BaseServiceUnitTest {

  private IamToken userToken1;
  private IamToken userToken2;
  private IamToken profileToken1;
  private IamToken profileToken2;
  private String url;

  private static final String API_KEY = "my-apikey";
  private static final String PROFILE_CRN = "my-profile-crn-1";
  private static final String PROFILE_ID = "my-profile-id-1";
  private static final String PROFILE_NAME = "my-profile-1";
  private static final String ACCOUNT_ID = "my-account-id-1";

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    userToken1 = loadFixture("src/test/resources/user_token1.json", IamToken.class);
    userToken2 = loadFixture("src/test/resources/user_token2.json", IamToken.class);
    profileToken1 = loadFixture("src/test/resources/profile_token1.json", IamToken.class);
    profileToken2 = loadFixture("src/test/resources/profile_token2.json", IamToken.class);
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
    new IamAssumeAuthenticator.Builder()
      .apikey(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyApiKey() {
    new IamAssumeAuthenticator.Builder()
      .apikey("")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInvalidApiKey() {
    new IamAssumeAuthenticator.Builder()
      .apikey("{apikey}")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingProfile() {
    new IamAssumeAuthenticator.Builder()
      .apikey(API_KEY)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDuplicateProfile1() {
    new IamAssumeAuthenticator.Builder()
      .apikey(API_KEY)
      .iamProfileCrn(PROFILE_CRN)
      .iamProfileId(PROFILE_ID)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDuplicateProfile2() {
    new IamAssumeAuthenticator.Builder()
      .apikey(API_KEY)
      .iamProfileId(PROFILE_ID)
      .iamProfileName(PROFILE_NAME)
      .iamAccountId(ACCOUNT_ID)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingAccountId() {
    new IamAssumeAuthenticator.Builder()
      .apikey(API_KEY)
      .iamProfileName(PROFILE_NAME)
      .build();
  }

  @Test
  public void testBasicConfiguration() {
    IamAssumeAuthenticator auth = new IamAssumeAuthenticator.Builder()
      .apikey(API_KEY)
      .iamProfileCrn(PROFILE_CRN)
      .build();
    assertNotNull(auth);
    assertEquals(auth.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
  }

  @Test
  public void testBuilderCorrectConfig() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN)
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
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    assertEquals(authenticator.getIamProfileCrn(), PROFILE_CRN);
    assertEquals(authenticator.getURL(), "url");
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(authenticator.getHeaders(), expectedHeaders);
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    IamAssumeAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_IAM_PROFILE_ID, PROFILE_ID);
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, "url");
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    IamAssumeAuthenticator authenticator = IamAssumeAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    assertEquals(authenticator.getIamProfileId(), PROFILE_ID);
    assertEquals(authenticator.getURL(), "url");
    assertTrue(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_IAM_PROFILE_NAME, PROFILE_NAME);
    props.put(Authenticator.PROPNAME_IAM_ACCOUNT_ID, ACCOUNT_ID);
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);

    IamAssumeAuthenticator authenticator = IamAssumeAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_IAM_ASSUME);
    assertEquals(authenticator.getIamProfileName(), PROFILE_NAME);
    assertEquals(authenticator.getIamAccountId(), ACCOUNT_ID);
    assertNotNull(authenticator.getURL());
    assertFalse(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testDisableSSLVerification() {
    IamAssumeAuthenticator auth = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN).apikey(API_KEY).build();
    assertFalse(auth.getDisableSSLVerification());
    auth.setDisableSSLVerification(true);
    assertTrue(auth.getDisableSSLVerification());

    auth = new IamAssumeAuthenticator.Builder()
        .iamProfileId(PROFILE_ID).apikey(API_KEY).disableSSLVerification(true).build();
    assertTrue(auth.getDisableSSLVerification());
  }

  //
  // Tests involving interactions with a mocked token service.
  //

  @Test
  public void testAuthenticateCachedToken() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));

    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN)
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
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Set mock time to be within the token's lifetime, but before the refresh window.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(profileToken1.getExpiration() - 1000L);

    // Authenticator should use the cached access token this time.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticateExpiredToken() {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));
    server.enqueue(jsonResponse(userToken2));
    server.enqueue(jsonResponse(profileToken2));

    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileId(PROFILE_ID)
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // The authenticator should request a new, valid access token (profileToken1).
    // This will consume the first two responses: userToken, profileToken1
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now set the mock time to reflect that the first access token ("profileToken1") has expired.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(profileToken1.getExpiration());

    // Authenticator should detect the expiration and request a new access token when we call authenticate() again.
    // This will consume the next two responses: userToken2, profileToken2
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken2.getAccessToken());
  }

  @Test
  public void testAuthenticateExpiredToken10SecWindow() {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));
    server.enqueue(jsonResponse(userToken2));
    server.enqueue(jsonResponse(profileToken2));


    // Set initial mock time to be epoch time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileName(PROFILE_NAME)
        .iamAccountId(ACCOUNT_ID)
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // The authenticator should request a new, valid access token (profileToken1).
    // This will consume the first two responses: userToken1, profileToken1
    authenticator.authenticate(requestBuilder);

    // Now set the mock time to reflect so that "profileToken1" will appear to be expired.
    // We subtract 10s from the expiration time to test the boundary condition of the expiration window feature.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(
        profileToken1.getExpiration() - IamToken.IamExpirationWindow);

    // The authenticator should detect the expiration and request a new access token when we call authenticate() again.
    // This will consome the next two responses: userToken2, profileToken2
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken2.getAccessToken());
  }

  @Test
  public void testAuthenticateBackgroundTokenRefresh() throws InterruptedException {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));
    server.enqueue(jsonResponse(userToken2));
    server.enqueue(jsonResponse(profileToken2));

    // Set initial mock time to be epoch time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN)
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // The authenticator should fetch the first access token (profileToken1).
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now set the mock time to put us in the "refresh window" where the token is not expired,
    // but still needs to be refreshed.
    long refreshWindow = (long) (profileToken1.getExpiresIn() * 0.2);
    long refreshTime = profileToken1.getExpiration() - refreshWindow;
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(refreshTime + 2L);

    // The authenticator should detect the need to refresh in the background while still using the cached access token
    // (profileToken1) when we call authenticate() again.
    // The immediate response should be the cached access token (profileToken1) since it hasn't expired yet.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Next the authenticator should use the new token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken2.getAccessToken());
  }

  @Test
  public void testUserHeaders() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    headers.put("Host", "iam.cloud.ibm.com:81");
    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileId(PROFILE_ID)
        .apikey(API_KEY)
        .url(url)
        .headers(headers)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now do some validation on the mock requests sent to the token server.
    assertEquals(server.getRequestCount(), 2);

    RecordedRequest firstRequest = server.takeRequest();
    assertNotNull(firstRequest);
    assertNotNull(firstRequest.getHeaders());
    Headers actualHeaders = firstRequest.getHeaders();
    assertEquals(actualHeaders.get("header1"), "value1");
    assertEquals(actualHeaders.get("header2"), "value2");
    assertEquals("iam.cloud.ibm.com:81", actualHeaders.get("Host"));
    assertTrue(actualHeaders.get(HttpHeaders.USER_AGENT).startsWith("ibm-java-sdk-core/iam-authenticator"));

    RecordedRequest secondRequest = server.takeRequest();
    assertNotNull(secondRequest);
    assertNotNull(secondRequest.getHeaders());
    actualHeaders = secondRequest.getHeaders();
    assertEquals(actualHeaders.get("header1"), "value1");
    assertEquals(actualHeaders.get("header2"), "value2");
    assertEquals(actualHeaders.get("Host"), "iam.cloud.ibm.com:81");
    assertTrue(actualHeaders.get(HttpHeaders.USER_AGENT).startsWith("ibm-java-sdk-core/iam-assume-authenticator"));

    // The authenticator should use the cached access token (profileToken1) since it hasn't expired yet.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());
  }

  @Test
  public void testFormBodyProfileName() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileName(PROFILE_NAME)
        .iamAccountId(ACCOUNT_ID)
        .apikey(API_KEY)
        .url(url)
        .build();
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now do some validation on the mock requests sent to the token server.
    RecordedRequest firstRequest = server.takeRequest();
    assertNotNull(firstRequest);
    String body = firstRequest.getBody().readUtf8();
    String expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=" + API_KEY + "&response_type=cloud_iam";
    assertEquals(body, expectedBody);

    RecordedRequest secondRequest = server.takeRequest();
    assertNotNull(secondRequest);
    body = secondRequest.getBody().readUtf8();
    expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aassume&access_token="
        + userToken1.getAccessToken() + "&profile_name=" + PROFILE_NAME + "&account=" + ACCOUNT_ID;
    assertEquals(body, expectedBody);
  }

  @Test
  public void testFormBodyProfileCrn() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN)
        .apikey(API_KEY)
        .url(url)
        .build();
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now do some validation on the mock requests sent to the token server.
    RecordedRequest firstRequest = server.takeRequest();
    assertNotNull(firstRequest);
    String body = firstRequest.getBody().readUtf8();
    String expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=" + API_KEY + "&response_type=cloud_iam";
    assertEquals(body, expectedBody);

    RecordedRequest secondRequest = server.takeRequest();
    assertNotNull(secondRequest);
    body = secondRequest.getBody().readUtf8();
    expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aassume&access_token="
        + userToken1.getAccessToken() + "&profile_crn=" + PROFILE_CRN;
    assertEquals(body, expectedBody);
  }

  @Test
  public void testFormBodyProfileId() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse(profileToken1));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileId(PROFILE_ID)
        .apikey(API_KEY)
        .url(url)
        .build();
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + profileToken1.getAccessToken());

    // Now do some validation on the mock requests sent to the token server.
    RecordedRequest firstRequest = server.takeRequest();
    assertNotNull(firstRequest);
    String body = firstRequest.getBody().readUtf8();
    String expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=" + API_KEY + "&response_type=cloud_iam";
    assertEquals(body, expectedBody);

    RecordedRequest secondRequest = server.takeRequest();
    assertNotNull(secondRequest);
    body = secondRequest.getBody().readUtf8();
    expectedBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aassume&access_token="
        + userToken1.getAccessToken() + "&profile_id=" + PROFILE_ID;
    assertEquals(body, expectedBody);
  }

  @Test(expectedExceptions = ServiceResponseException.class)
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileCrn(PROFILE_CRN)
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    authenticator.authenticate(requestBuilder);
  }

  @Test
  public void testApiResponseError() throws Throwable {
    server.enqueue(jsonResponse(userToken1));
    server.enqueue(jsonResponse("{'}"));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    IamAssumeAuthenticator authenticator = new IamAssumeAuthenticator.Builder()
        .iamProfileId(PROFILE_ID)
        .apikey(API_KEY)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    try {
      authenticator.authenticate(requestBuilder);
      fail("Expected authenticate() to throw an exception!");
    } catch (RuntimeException excp) {
      Throwable causedBy = excp.getCause();
      assertNotNull(causedBy);
      assertTrue(causedBy instanceof IllegalStateException);
    } catch (Throwable t) {
      fail("Expected RuntimeException, not " + t.getClass().getSimpleName());
    }
  }
}
