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

import java.nio.file.NoSuchFileException;
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
import com.ibm.cloud.sdk.core.security.AuthenticatorBase;
import com.ibm.cloud.sdk.core.security.ContainerAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class ContainerAuthenticatorTest extends BaseServiceUnitTest {

  private IamToken tokenData1;
  private IamToken tokenData2;
  private String url;

  private static final String mockCRTokenFile = "src/test/resources/cr-token.txt";
  private static final String mockIamProfileName = "iam-user-123";
  private static final String mockIamProfileId = "iam-id-123";
  private static final String mockClientId = "client-id-1";
  private static final String mockClientSecret = "client-secret-1";
  private static final String mockScope = "scope1";
  private static final String mockCRToken = "cr-token-1";

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData1 = loadFixture("src/test/resources/iam_token.json", IamToken.class);
    tokenData2 = loadFixture("src/test/resources/refreshed_iam_token.json", IamToken.class);
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
  public void testMissingProfileNameAndId() {
    new ContainerAuthenticator.Builder()
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyProfileNameAndId() {
    new ContainerAuthenticator.Builder()
      .iamProfileName("")
      .iamProfileId("")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingClientId() {
    new ContainerAuthenticator.Builder()
      .iamProfileName(mockIamProfileName)
      .clientId(null)
      .clientSecret(mockClientSecret)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyClientId() {
    new ContainerAuthenticator.Builder()
      .iamProfileId(mockIamProfileId)
      .clientId("")
      .clientSecret(mockClientSecret)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingClientSecret() {
    new ContainerAuthenticator.Builder()
      .iamProfileName(mockIamProfileName)
      .clientId(mockClientId)
      .clientSecret(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyClientSecret() {
    new ContainerAuthenticator.Builder()
      .iamProfileName(mockIamProfileName)
      .clientId(mockClientId)
      .clientSecret("")
      .build();
  }

  @Test
  public void testBuilderCorrectConfig() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .iamProfileId(mockIamProfileId)
        .url("url")
        .clientId(mockClientId)
        .clientSecret(mockClientSecret)
        .scope(mockScope)
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .proxy(null)
        .proxyAuthenticator(null)
        .build();
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    assertEquals(authenticator.getCrTokenFilename(), mockCRTokenFile);
    assertEquals(authenticator.getIamProfileName(), mockIamProfileName);
    assertEquals(authenticator.getIamProfileId(), mockIamProfileId);
    assertEquals(authenticator.getURL(), "url");
    assertEquals(authenticator.getClientId(), mockClientId);
    assertEquals(authenticator.getClientSecret(), mockClientSecret);
    assertEquals(authenticator.getScope(), mockScope);
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(authenticator.getHeaders(), expectedHeaders);
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    ContainerAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);

    auth2 = new ContainerAuthenticator.Builder()
        .iamProfileName(mockIamProfileName)
        .build();
    assertNotNull(auth2);
    assertNotNull(auth2.getURL());
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_CR_TOKEN_FILENAME, mockCRTokenFile);
    props.put(Authenticator.PROPNAME_IAM_PROFILE_NAME, mockIamProfileName);
    props.put(Authenticator.PROPNAME_IAM_PROFILE_ID, mockIamProfileId);
    props.put(Authenticator.PROPNAME_URL, "url");
    props.put(Authenticator.PROPNAME_CLIENT_ID, mockClientId);
    props.put(Authenticator.PROPNAME_CLIENT_SECRET, mockClientSecret);
    props.put(Authenticator.PROPNAME_SCOPE, mockScope);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    ContainerAuthenticator authenticator = ContainerAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    assertEquals(authenticator.getCrTokenFilename(), mockCRTokenFile);
    assertEquals(authenticator.getIamProfileName(), mockIamProfileName);
    assertEquals(authenticator.getIamProfileId(), mockIamProfileId);
    assertEquals(authenticator.getURL(), "url");
    assertEquals(authenticator.getClientId(), mockClientId);
    assertEquals(authenticator.getClientSecret(), mockClientSecret);
    assertEquals(authenticator.getScope(), mockScope);
    assertTrue(authenticator.getDisableSSLVerification());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_IAM_PROFILE_NAME, mockIamProfileName);
    props.put(Authenticator.PROPNAME_SCOPE, mockScope);

    ContainerAuthenticator authenticator = ContainerAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_CONTAINER);
    assertNull(authenticator.getCrTokenFilename());
    assertEquals(authenticator.getIamProfileName(), mockIamProfileName);
    assertNull(authenticator.getIamProfileId());
    assertEquals(authenticator.getScope(), mockScope);
    assertNotNull(authenticator.getURL());
    assertNull(authenticator.getClientId());
    assertNull(authenticator.getClientSecret());
    assertFalse(authenticator.getDisableSSLVerification());
  }

  //
  // Tests involving interactions with a mocked token service.
  //

  @Test
  public void testAuthenticateNewAndStoredToken() throws Throwable {
    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
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

    // Set mock server response.
    server.enqueue(jsonResponse(tokenData1));

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Validate parts of the IAM request that was sent as a result of the authenticate() call above.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);

    // Make sure we do not see an Authorization header in the IAM request since
    // we didn't configure clientId/clientSecret.
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNotNull(actualHeaders);
    assertNull(actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Validate the form params included in the request.
    Map<String, String> formBody = getFormBodyAsMap(tokenServerRequest);
    assertEquals(formBody.get("cr_token"), mockCRToken);
    assertEquals(formBody.get("grant_type"), "urn:ibm:params:oauth:grant-type:cr-token");
    assertEquals(formBody.get("profile_name"), mockIamProfileName);
    assertFalse(formBody.containsKey("profile_id"));
    assertFalse(formBody.containsKey("scope"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticationExpiredToken() throws Throwable {
    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);


    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // This will bootstrap the test by forcing the Authenticator to retrieve the first access token,
    // which will appear as expired when we call authenticate() the second time below.
    authenticator.authenticate(requestBuilder);

    // Validate parts of the IAM request that was sent as a result of the authenticate() call above.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);

    // Validate the form params included in the request.
    Map<String, String> formBody = getFormBodyAsMap(tokenServerRequest);
    assertEquals(formBody.get("cr_token"), mockCRToken);
    assertEquals(formBody.get("grant_type"), "urn:ibm:params:oauth:grant-type:cr-token");
    assertEquals(formBody.get("profile_id"), mockIamProfileId);
    assertFalse(formBody.containsKey("profile_name"));
    assertFalse(formBody.containsKey("scope"));

    // Now set the mock time to reflect that the first access token ("tokenData1") has expired.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(tokenData1.getExpiration());

    // Set mock server to return second access token.
    server.enqueue(jsonResponse(tokenData2));

    // Authenticator should detect the expiration and request a
    // new access token when we call authenticate() again.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData2.getAccessToken());
  }

  @Test
  public void testAuthenticationExpiredToken10SecWindow() throws Throwable {
    // Mock current time to ensure that we're way before the first token's expiration time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);


    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // This will bootstrap the test by forcing the Authenticator to retrieve the first access token,
    // which will appear as expired when we call authenticate() the second time below.
    authenticator.authenticate(requestBuilder);

    // Validate parts of the IAM request that was sent as a result of the authenticate() call above.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);

    // Validate the form params included in the request.
    Map<String, String> formBody = getFormBodyAsMap(tokenServerRequest);
    assertEquals(formBody.get("cr_token"), mockCRToken);
    assertEquals(formBody.get("grant_type"), "urn:ibm:params:oauth:grant-type:cr-token");
    assertEquals(formBody.get("profile_id"), mockIamProfileId);
    assertFalse(formBody.containsKey("profile_name"));
    assertFalse(formBody.containsKey("scope"));

    // Now set the mock time to reflect that the first access token ("tokenData") is considered to be "expired".
    // We subtract 10s from the expiration time to test the boundary condition of the expiration window feature.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(
        tokenData1.getExpiration() - IamToken.IamExpirationWindow);

    // Set mock server to return second access token.
    server.enqueue(jsonResponse(tokenData2));

    // Authenticator should detect the expiration and request a
    // new access token when we call authenticate() again.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData2.getAccessToken());
  }

  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    // Set initial mock time to be epoch time.
    // This is because initially we have no access token at all so we should fetch one regardless of the current time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(0L);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // This will bootstrap the test by forcing the Authenticator to retrieve the first access token,
    // which will appear as being within the refresh window.
    authenticator.authenticate(requestBuilder);

    // Validate parts of the IAM request that was sent as a result of the authenticate() call above.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);

    // Validate the form params included in the request.
    Map<String, String> formBody = getFormBodyAsMap(tokenServerRequest);
    assertEquals(formBody.get("cr_token"), mockCRToken);
    assertEquals(formBody.get("grant_type"), "urn:ibm:params:oauth:grant-type:cr-token");
    assertEquals(formBody.get("profile_name"), mockIamProfileName);
    assertEquals(formBody.get("profile_id"), mockIamProfileId);
    assertFalse(formBody.containsKey("scope"));

    // Now set the mock time to put us in the "refresh window" where the token is not expired,
    // but still needs to be refreshed.
    long refreshWindow = (long) (tokenData1.getExpiresIn() * 0.2);
    long refreshTime = tokenData1.getExpiration() - refreshWindow;
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn(refreshTime + 2L);

    // Authenticator should detect the need to refresh and request a new access token
    // IN THE BACKGROUND when we call authenticate() again.
    // The immediate response should be the token which was already stored, since it's not yet
    // expired.
    server.enqueue(jsonResponse(tokenData2).setBodyDelay(2, TimeUnit.SECONDS));
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Next request should use the refreshed token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData2.getAccessToken());
  }


  @Test
  public void testUserHeaders() throws Throwable {
    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    headers.put("Host", "iam.cloud.ibm.com:81");

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
        .headers(headers)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Now do some validation on the mock request sent to the token server.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNotNull(actualHeaders);
    assertEquals(actualHeaders.get("header1"), "value1");
    assertEquals(actualHeaders.get("header2"), "value2");
    assertEquals(actualHeaders.get("Host"), "iam.cloud.ibm.com:81");
    assertTrue(actualHeaders.get(HttpHeaders.USER_AGENT).startsWith("ibm-java-sdk-core/container-authenticator"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());
  }

  @Test
  public void testClientIdSecret() throws Throwable {
    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
        .clientId(mockClientId)
        .clientSecret(mockClientSecret)
        .build();

    String expectedIAMAuthHeader = AuthenticatorBase.constructBasicAuthHeader(mockClientId, mockClientSecret);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Now make sure the token server request contained the correct Authorization header.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNotNull(actualHeaders);
    assertEquals(actualHeaders.get(HttpHeaders.AUTHORIZATION), expectedIAMAuthHeader);

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());
  }

  @Test
  public void testScope() throws Throwable {
    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
        .scope(mockScope)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return first access token.
    server.enqueue(jsonResponse(tokenData1));

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());

    // Now make sure the token server request contained the correct Authorization header.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNotNull(actualHeaders);
    assertNull(actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Validate the form params included in the request.
    Map<String, String> formBody = getFormBodyAsMap(tokenServerRequest);
    assertEquals(formBody.get("cr_token"), mockCRToken);
    assertEquals(formBody.get("grant_type"), "urn:ibm:params:oauth:grant-type:cr-token");
    assertEquals(formBody.get("profile_name"), mockIamProfileName);
    assertFalse(formBody.containsKey("profile_id"));
    assertEquals(formBody.get("scope"), mockScope);

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData1.getAccessToken());
  }

  @Test(expectedExceptions = ServiceResponseException.class)
  public void testApiErrorBadRequest() throws Throwable {

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return a 400 Bad Request response.
    server.enqueue(errorResponse(400));

    // Calling authenticate should result in an exception.
    authenticator.authenticate(requestBuilder);
  }

  @Test
  public void testApiResponseError() throws Throwable {
    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename(mockCRTokenFile)
        .iamProfileName(mockIamProfileName)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return an invalid JSON response body.
    server.enqueue(jsonResponse("{'}"));

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

  @Test
  public void testErrorReadingCRToken() throws Throwable {

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    ContainerAuthenticator authenticator = new ContainerAuthenticator.Builder()
        .crTokenFilename("bogus-cr-token-file")
        .iamProfileName(mockIamProfileName)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return the first access token, but we shouldn't get that far.
    server.enqueue(jsonResponse(tokenData1));

    // Calling authenticate should result in an exception.
    try {
      authenticator.authenticate(requestBuilder);
      fail("Expected authenticate() to result in exception!");
    } catch (RuntimeException excp) {
      Throwable causedBy = excp.getCause();
      assertNotNull(causedBy);
      assertTrue(causedBy instanceof NoSuchFileException);
    } catch (Throwable t) {
      fail("Expected RuntimeException, not " + t.getClass().getSimpleName());
    }
  }
}
