/**
 * (C) Copyright IBM Corp. 2019, 2023.
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

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataServiceInstanceAuthenticator;
import com.ibm.cloud.sdk.core.security.Cp4dServiceInstanceTokenResponse;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class Cp4dServiceInstanceAuthenticatorTest extends BaseServiceUnitTest {

  // Token with issued-at time of 1574353085 and expiration time of 1574453956
  private Cp4dServiceInstanceTokenResponse tokenData;

  // Token with issued-at time of 1602788645 and expiration time of 1999999999
  private Cp4dServiceInstanceTokenResponse refreshedTokenData;

  private String url;
  private String testUsername = "test-username";
  private String testApikey = "test-apikey";
  private String testServiceInstanceId = "test-serviceInstanceId";

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData = loadFixture("src/test/resources/cp4d_service_instance_token.json",
        Cp4dServiceInstanceTokenResponse.class);
    refreshedTokenData = loadFixture("src/test/resources/refreshed_cp4d_service_instance_token.json",
        Cp4dServiceInstanceTokenResponse.class);
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
  public void testBuilderMissingURL() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url(null)
      .username(testUsername)
      .apikey(testApikey)
      .serviceInstanceId(testServiceInstanceId)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderEmptyURL() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("")
      .username(testUsername)
      .apikey(testApikey)
      .serviceInstanceId(testServiceInstanceId)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderMissingUsername() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username(null)
      .apikey(testApikey)
      .serviceInstanceId(testServiceInstanceId)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderEmptyUsername() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username("")
      .apikey(testApikey)
      .serviceInstanceId(testServiceInstanceId)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderMissingApikey() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderEmptyApikey() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey("")
      .serviceInstanceId(testServiceInstanceId)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderMissingServiceInstanceId() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey(testApikey)
      .serviceInstanceId(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderEmptyServiceInstanceId() {
    new CloudPakForDataServiceInstanceAuthenticator.Builder()
      .url("https://good-url")
      .username(testUsername)
      .apikey("")
      .serviceInstanceId("")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingUrl() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, null);
    props.put(Authenticator.PROPNAME_USERNAME, "testUsername");
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyUrl() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "");
    props.put(Authenticator.PROPNAME_USERNAME, "testUsername");
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, null);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, "");
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingApikey() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, null);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyApikey() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, "");
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingServiceInstanceId() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, null);
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyServiceInstanceId() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, "");
    CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
  }

  @Test
  public void testBuilderCorrectConfig1() {
    CloudPakForDataServiceInstanceAuthenticator authenticator =
    		new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testServiceInstanceId, authenticator.getServiceInstanceId());
    assertEquals(testApikey, authenticator.getApikey());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig2() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    CloudPakForDataServiceInstanceAuthenticator authenticator =
    		new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .proxy(null)
        .proxyAuthenticator(null)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testServiceInstanceId, authenticator.getServiceInstanceId());
    assertEquals(testApikey, authenticator.getApikey());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    CloudPakForDataServiceInstanceAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    CloudPakForDataServiceInstanceAuthenticator authenticator =
    		CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testApikey, authenticator.getApikey());
    assertEquals(testServiceInstanceId, authenticator.getServiceInstanceId());
    assertTrue(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_APIKEY, testApikey);
    props.put(Authenticator.PROPNAME_SERVICE_INSTANCE_ID, testServiceInstanceId);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceInstanceAuthenticator authenticator =
    		CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testApikey, authenticator.getApikey());
    assertEquals(testServiceInstanceId, authenticator.getServiceInstanceId());
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
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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

    // Authenticator should request new, valid token.
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getData().getToken());

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getData().getToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure that we've passed the token expiration time.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 1800000000);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getData().getToken());
  }

  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to put us in the "refresh window" where the token is not expired but still needs refreshed.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 1574453700);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getData().getToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Next request should use the refreshed token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getData().getToken());
  }

  @Test
  public void testUserHeaders() throws Throwable {
    server.enqueue(jsonResponse(tokenData));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getData().getToken());

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
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getData().getToken());
  }

  @Test
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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
    clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceInstanceAuthenticator authenticator =
        new CloudPakForDataServiceInstanceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .apikey(testApikey)
        .serviceInstanceId(testServiceInstanceId)
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
