/**
 * (C) Copyright IBM Corp. 2022.
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

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.CloudPakForDataServiceAuthenticator;
import com.ibm.cloud.sdk.core.security.Cp4dTokenResponse;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

@PrepareForTest({ Clock.class })
@PowerMockIgnore({
    "javax.net.ssl.*",
    "okhttp3.*",
    "okio.*"
})
public class Cp4dServiceAuthenticatorTest extends BaseServiceUnitTest {

  // Token with issued-at time of 1574353085 and expiration time of 1574453956
  private Cp4dTokenResponse tokenData;

  // Token with issued-at time of 1602788645 and expiration time of 1999999999
  private Cp4dTokenResponse refreshedTokenData;

  private String url;
  private String testUsername = "test-username";
  private String testServiceBrokerSecret = "f8b7czjt701wy6253be5q8ad8f07kd08";
  private String testDisplayName = "test-displayname";
  private String testUid = "test-uid";
  private String testPermissions = "read,write";
  private String testExpirationTime = "5";

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    tokenData = loadFixture("src/test/resources/cp4d_token.json", Cp4dTokenResponse.class);
    refreshedTokenData = loadFixture("src/test/resources/refreshed_cp4d_token.json", Cp4dTokenResponse.class);
  }

  //
  // Tests involving the new Builder class and fromConfiguration() method.
  //

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderMissingURL() {
    new CloudPakForDataServiceAuthenticator.Builder()
      .url(null)
      .username(testUsername)
      .serviceBrokerSecret(testServiceBrokerSecret)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderEmptyURL() {
	new CloudPakForDataServiceAuthenticator.Builder()
      .url("")
      .username(testUsername)
      .serviceBrokerSecret(testServiceBrokerSecret)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderInvalidExpirationTime() {
	new CloudPakForDataServiceAuthenticator.Builder()
      .url("https://good-url")
      .serviceBrokerSecret(testServiceBrokerSecret)
      .expirationTime("invalid")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderMissingServiceBrokerSecret() {
	new CloudPakForDataServiceAuthenticator.Builder()
      .url("https://good-url")
      .serviceBrokerSecret(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingUrl() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, null);
    props.put(Authenticator.PROPNAME_USERNAME, "testUsername");
    CloudPakForDataServiceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingServiceBrokerSecret() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, null);
    CloudPakForDataServiceAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigInvalidExpirationTime() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, "https://good-url");
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_EXPIRATION_TIME, "invalid");
    CloudPakForDataServiceAuthenticator.fromConfiguration(props);
  }

  @Test
  public void testBuilderCorrectConfig1() {
    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testServiceBrokerSecret, authenticator.getServiceBrokerSecret());
    assertNull(authenticator.getUsername());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getUid());
    assertNull(authenticator.getDisplayName());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig2() {
	CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getUid());
    assertNull(authenticator.getDisplayName());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig3() {
	CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .uid(testUid)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .headers(null)
        .proxy(null)
        .proxyAuthenticator(null)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getDisplayName());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
    assertNull(authenticator.getProxy());
    assertNull(authenticator.getProxyAuthenticator());

    CloudPakForDataServiceAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);
  }

  @Test
  public void testBuilderCorrectConfig4() {
	CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .uid(testUid)
        .displayName(testDisplayName)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig5() {
	CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .uid(testUid)
        .displayName(testDisplayName)
        .permissions(testPermissions)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertEquals(testPermissions, authenticator.getPermissions());
    assertNull(authenticator.getExpirationTime());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig6() {
	CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .username(testUsername)
        .uid(testUid)
        .displayName(testDisplayName)
        .permissions(testPermissions)
        .expirationTime(testExpirationTime)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertEquals(testPermissions, authenticator.getPermissions());
    assertEquals(testExpirationTime, authenticator.getExpirationTime());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testBuilderCorrectConfig7() {
    Map<String, String> expectedHeaders = new HashMap<>();
    expectedHeaders.put("header1", "value1");
    expectedHeaders.put("header2", "value2");

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .disableSSLVerification(true)
        .headers(expectedHeaders)
        .build();
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testServiceBrokerSecret, authenticator.getServiceBrokerSecret());
    assertNull(authenticator.getUsername());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getUid());
    assertNull(authenticator.getDisplayName());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals(expectedHeaders, authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testServiceBrokerSecret, authenticator.getServiceBrokerSecret());
    assertNull(authenticator.getUsername());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getUid());
    assertNull(authenticator.getDisplayName());
    assertTrue(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertNull(authenticator.getUid());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getDisplayName());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig3() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_UID, testUid);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertNull(authenticator.getDisplayName());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig4() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_UID, testUid);
    props.put(Authenticator.PROPNAME_DISPLAY_NAME, testDisplayName);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertNull(authenticator.getExpirationTime());
    assertNull(authenticator.getPermissions());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig5() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_UID, testUid);
    props.put(Authenticator.PROPNAME_DISPLAY_NAME, testDisplayName);
    props.put(Authenticator.PROPNAME_PERMISSIONS, testPermissions);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertEquals(testPermissions, authenticator.getPermissions());
    assertNull(authenticator.getExpirationTime());
    assertFalse(authenticator.getDisableSSLVerification());
    assertNull(authenticator.getHeaders());
  }

  @Test
  public void testConfigCorrectConfig6() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_URL, url);
    props.put(Authenticator.PROPNAME_SERVICE_BROKER_SECRET, testServiceBrokerSecret);
    props.put(Authenticator.PROPNAME_USERNAME, testUsername);
    props.put(Authenticator.PROPNAME_UID, testUid);
    props.put(Authenticator.PROPNAME_DISPLAY_NAME, testDisplayName);
    props.put(Authenticator.PROPNAME_PERMISSIONS, testPermissions);
    props.put(Authenticator.PROPNAME_EXPIRATION_TIME, testExpirationTime);
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "false");

    CloudPakForDataServiceAuthenticator authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    assertEquals(Authenticator.AUTHTYPE_CP4D_SERVICE, authenticator.authenticationType());
    assertEquals(url, authenticator.getURL());
    assertEquals(testUsername, authenticator.getUsername());
    assertEquals(testUid, authenticator.getUid());
    assertEquals(testDisplayName, authenticator.getDisplayName());
    assertEquals(testPermissions, authenticator.getPermissions());
    assertEquals(testExpirationTime, authenticator.getExpirationTime());
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

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .disableSSLVerification(true)
        .username(testUsername)
        .uid(testUid)
        .displayName(testDisplayName)
        .permissions(testPermissions)
        .expirationTime(testExpirationTime)
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
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 1800000000);

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
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

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
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

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .disableSSLVerification(true)
        .build();

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    headers.put("Host", "cp4d.cloud.ibm.com:81");
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
    assertEquals("cp4d.cloud.ibm.com:81", actualHeaders.get("Host"));

    // Authenticator should just return the same token this time since we have a valid one stored.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + tokenData.getToken());
  }

  @Test(expectedExceptions = ServiceResponseException.class)
  public void testApiErrorBadRequest() throws Throwable {
    server.enqueue(errorResponse(400));

    // Mock current time to ensure the token is valid.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Calling authenticate should result in an exception.
    authenticator.authenticate(requestBuilder);
  }

  @Test
  public void testApiErrorResponse() throws Throwable {
    server.enqueue(jsonResponse("{'}"));

    // Mock current time to ensure the token is valid.
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

    CloudPakForDataServiceAuthenticator authenticator = new CloudPakForDataServiceAuthenticator.Builder()
        .url(url)
        .serviceBrokerSecret(testServiceBrokerSecret)
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
