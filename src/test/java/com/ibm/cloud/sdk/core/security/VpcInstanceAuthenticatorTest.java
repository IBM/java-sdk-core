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

package com.ibm.cloud.sdk.core.security;

import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;
import static org.testng.Assert.assertEquals;
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

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
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
public class VpcInstanceAuthenticatorTest extends BaseServiceUnitTest {
  private VpcTokenResponse vpcInstanceIdentityTokenResponse;
  private VpcTokenResponse vpcIamAccessTokenResponse1;
  private VpcTokenResponse vpcIamAccessTokenResponse2;
  private String url;

  private static final String mockIamProfileCrn = "crn:iam-profile:123";
  private static final String mockIamProfileId = "iam-id-123";

  private static final String mockErrorResponseJson1 =
      "{\"errors\": [{\"message\": \"Your create_access_token request was bad.\", \"code\": \"invalid_parameter_value\"}]}";
  private static final String mockErrorResponseJson2 =
      "{\"errors\": [{\"message\": \"Your create_iam_token request was bad.\", \"code\": \"invalid_parameter_value\"}]}";

  // Logging level used by this test.
  // For debugging, set this to Level.FINE or Level.ALL, etc.
  private static Level logLevel = Level.SEVERE;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();

    // Read in and unmarshal our mock VPC API responses.
    vpcInstanceIdentityTokenResponse = loadFixture("src/test/resources/vpc_iit_response.json", VpcTokenResponse.class);
    vpcIamAccessTokenResponse1 = loadFixture("src/test/resources/vpc_iam_access_response1.json", VpcTokenResponse.class);
    vpcIamAccessTokenResponse2 = loadFixture("src/test/resources/vpc_iam_access_response2.json", VpcTokenResponse.class);

    // Set up java.util.logging to display messages on the console.
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(logLevel);
    Logger logger;
    logger = Logger.getLogger(VpcInstanceAuthenticator.class.getName());
    logger.setLevel(logLevel);
    logger.addHandler(handler);

    logger = Logger.getLogger(TokenRequestBasedAuthenticator.class.getName());
    logger.setLevel(logLevel);
    logger.addHandler(handler);
  }


  //
  // Tests involving the Builder class and fromConfiguration() method.
  //

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuilderErrorProfileCrnAndId() {
    new VpcInstanceAuthenticator.Builder()
      .iamProfileCrn(mockIamProfileCrn)
      .iamProfileId(mockIamProfileId)
      .build();
  }

  @Test
  public void testBuilderDefaultConfig() {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
      .build();
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_VPC);
    assertNull(authenticator.getURL());
    assertNull(authenticator.getIamProfileCrn());
    assertNull(authenticator.getIamProfileId());

    VpcInstanceAuthenticator auth2 = authenticator.newBuilder().build();
    assertNotNull(auth2);
  }

  @Test
  public void testBuilderCorrectConfig1() {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url("url")
        .build();
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_VPC);
    assertEquals(authenticator.getIamProfileCrn(), mockIamProfileCrn);
    assertNull(authenticator.getIamProfileId());
    assertEquals(authenticator.getURL(), "url");
  }

  @Test
  public void testBuilderCorrectConfig2() {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url("url")
        .build();
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_VPC);
    assertNull(authenticator.getIamProfileCrn());
    assertEquals(authenticator.getIamProfileId(), mockIamProfileId);
    assertEquals(authenticator.getURL(), "url");
  }

  @Test
  public void testConfigCorrectConfig1() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_IAM_PROFILE_CRN, mockIamProfileCrn);
    props.put(Authenticator.PROPNAME_URL, "url");

    VpcInstanceAuthenticator authenticator = VpcInstanceAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_VPC);
    assertEquals(authenticator.getIamProfileCrn(), mockIamProfileCrn);
    assertNull(authenticator.getIamProfileId());
    assertEquals(authenticator.getURL(), "url");
  }

  @Test
  public void testConfigCorrectConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_IAM_PROFILE_ID, mockIamProfileId);
    props.put(Authenticator.PROPNAME_URL, "url");

    VpcInstanceAuthenticator authenticator = VpcInstanceAuthenticator.fromConfiguration(props);
    assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_VPC);
    assertNull(authenticator.getIamProfileCrn());
    assertEquals(authenticator.getIamProfileId(), mockIamProfileId);
    assertEquals(authenticator.getURL(), "url");
  }

  //
  // Tests involving the "retrieveInstanceIdentityToken()" method.
  //

  @Test
  public void testRetrieveInstanceIdentityTokenSuccess() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    // Set mock server to send back a good response.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));

    String instanceIdentityToken = authenticator.retrieveInstanceIdentityToken();
    assertNotNull(instanceIdentityToken);
    assertEquals(instanceIdentityToken, vpcInstanceIdentityTokenResponse.getAccessToken());
  }

  @Test
  public void testRetrieveInstanceIdentityTokenFailure() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a bad response.
    server.enqueue(errorResponse(400, mockErrorResponseJson1));

    // "retrieveInstanceIdentityToken()" should result in an exception.
    try {
      authenticator.retrieveInstanceIdentityToken();
      fail("Expected retrieveInstanceIdentityToken() to result in exception!");
    } catch (ServiceResponseException excp) {
      assertEquals(excp.getMessage(), "Your create_access_token request was bad.");
    } catch (Throwable t) {
      fail("Expected ServiceResponseException, not " + t.getClass().getSimpleName());
    }
  }

  //
  // Tests involving the "retrieveIamAccessToken()" method.
  //

  @Test
  public void testRetrieveIamAccessTokenSuccess() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    // Set mock server to send back a good response.
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));

    // Since we're testing just the IAM token exchange part of the process,
    // we'll use a fake instance identity token.
    String instanceIdentityToken = "vpc-token";
    IamToken iamToken = authenticator.retrieveIamAccessToken(instanceIdentityToken);
    assertNotNull(iamToken);
    assertEquals(iamToken.getAccessToken(), vpcIamAccessTokenResponse1.getAccessToken());

    // Now make sure the VPC "create_iam_token" request included an Authorization header
    // containing the instance identity token.
    RecordedRequest vpcIamTokenRequest = server.takeRequest();
    assertNotNull(vpcIamTokenRequest);
    Headers actualHeaders = vpcIamTokenRequest.getHeaders();
    assertNotNull(actualHeaders);
    assertEquals(actualHeaders.get(HttpHeaders.AUTHORIZATION), "Bearer " + instanceIdentityToken);
  }

  @Test
  public void testRetrieveIamAccessTokenFailure() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a bad response.
    server.enqueue(errorResponse(400, mockErrorResponseJson2));

    // "retrieveIamAccessToken()" should result in an exception,
    // but that exception should be stored in an IamToken instance rather
    // being thrown.
    IamToken iamToken = authenticator.retrieveIamAccessToken("vpc-token");
    assertNotNull(iamToken);
    assertNull(iamToken.getAccessToken());
    Throwable t = iamToken.getException();
    assertNotNull(t);
    assertTrue(t instanceof ServiceResponseException);
    ServiceResponseException s = (ServiceResponseException) t;
    assertEquals(s.getMessage(), "Your create_iam_token request was bad.");
  }

  //
  // Tests involving the requestToken() method.
  //

  @Test
  public void testRequestTokenSuccess() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    // Set mock server to send back good responses.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));

    IamToken iamToken = authenticator.requestToken();
    assertNotNull(iamToken);
    assertEquals(iamToken.getAccessToken(), vpcIamAccessTokenResponse1.getAccessToken());
  }

  @Test
  public void testRequestTokenFailure1() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a bad response for the "create_access_token" request.
    server.enqueue(errorResponse(400, mockErrorResponseJson1));

    // "requestToken()" should result in an exception,
    // but that exception should be stored in an IamToken instance rather
    // being thrown.
    IamToken iamToken = authenticator.requestToken();
    assertNotNull(iamToken);
    assertNull(iamToken.getAccessToken());
    Throwable t = iamToken.getException();
    assertNotNull(t);
    assertTrue(t instanceof ServiceResponseException);
    ServiceResponseException s = (ServiceResponseException) t;
    assertEquals(s.getMessage(), "Your create_access_token request was bad.");
  }

  @Test
  public void testRequestTokenFailure2() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a good response for the "create_access_token" request,
    // and then an error response for the "create_iam_token" request.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(errorResponse(400, mockErrorResponseJson2));

    // "requestToken()" should result in an exception,
    // but that exception should be stored in an IamToken instance rather
    // being thrown.
    IamToken iamToken = authenticator.requestToken();
    assertNotNull(iamToken);
    assertNull(iamToken.getAccessToken());
    Throwable t = iamToken.getException();
    assertNotNull(t);
    assertTrue(t instanceof ServiceResponseException);
    ServiceResponseException s = (ServiceResponseException) t;
    assertEquals(s.getMessage(), "Your create_iam_token request was bad.");
  }

  //
  // Tests involving the getToken() method.
  //

  @Test
  public void testGetTokenSuccess() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    // Set mock server to send back good responses.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));

    String iamAccessToken = authenticator.getToken();
    assertNotNull(iamAccessToken);
    assertEquals(iamAccessToken, vpcIamAccessTokenResponse1.getAccessToken());
  }

  @Test
  public void testGetTokenFailure1() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a bad response for the "create_access_token" request.
    server.enqueue(errorResponse(400, mockErrorResponseJson1));

    // "getToken()" should result in an exception.
    try {
      authenticator.getToken();
      fail("Expected getToken() to throw an exception!");
    } catch (ServiceResponseException s) {
      assertEquals(s.getMessage(), "Your create_access_token request was bad.");
    } catch (Throwable t) {
      fail("Expected ServiceResponseException, not " + t.getClass().getSimpleName());
    }
  }

  @Test
  public void testGetTokenFailure2() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileCrn(mockIamProfileCrn)
        .url(url)
        .build();

    // Set mock server to return a good response for the "create_access_token" request,
    // and then an error response for the "create_iam_token" request.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(errorResponse(400, mockErrorResponseJson2));


    // "getToken()" should result in an exception.
    try {
      authenticator.getToken();
      fail("Expected getToken() to throw an exception!");
    } catch (ServiceResponseException s) {
      assertEquals(s.getMessage(), "Your create_iam_token request was bad.");
    } catch (Throwable t) {
      fail("Expected ServiceResponseException, not " + t.getClass().getSimpleName());
    }
  }

  @Test
  public void testAuthenticateNewAndCachedToken() throws Throwable {
    // Mock current time to ensure that we're before the token expiration time.
    long mockTime = vpcIamAccessTokenResponse1.getCreatedAt().getTime() / 1000;
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn(mockTime);

    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
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

    // Set mock server responses.
    // We'll include a second set of responses, but those should not be used
    // because we're simulating the use of the cached token.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse2));

    // Calling "authenticate()" the first time should result in a new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse1.getAccessToken());

    // Calling "authenticate()" again should just re-use the cached token.
    requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse1.getAccessToken());

    // Verify that the authenticator is still using the same client instance that we set before.
    assertEquals(authenticator.getClient(), client);
  }

  @Test
  public void testAuthenticationExpiredToken() throws Throwable {
    // Mock current time to ensure that we're past the token expiration time.
    long mockTime = vpcIamAccessTokenResponse1.getExpiresAt().getTime() / 1000 + 1;
    PowerMockito.mockStatic(Clock.class);
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn(mockTime);

    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server responses.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse2));

    // Calling "authenticate()" the first time should result in a new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse1.getAccessToken());

    // Calling "authenticate()" again should result in a new access token
    // because we should detect that the first one obtained above has expired.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse2.getAccessToken());
  }


  @Test
  public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
    PowerMockito.mockStatic(Clock.class);

    // "expiresAt" will be the token's expiration time expressed as # of seconds since 1/1/1970.
    // "ttl10" will be set to 10% of the token's time to live in seconds.
    // "ttl25" will be set to 25% of the token's time to live in seconds.
    long expiresAt = vpcIamAccessTokenResponse1.getExpiresAt().getTime() / 1000;
    long ttl10 = (long) (0.1 * vpcIamAccessTokenResponse1.getExpiresIn());
    long ttl25 = (long) (0.25 * vpcIamAccessTokenResponse1.getExpiresIn());

    // Mock current time to put us in the "refresh window".
    // We'll do this by setting the clock to the expiration minus 10% of time to live.
    // The refresh window starts at expiration time minus 20% of time-to-live,
    // so "expiresAt - ttl10" puts us right in the middle of the refresh window.
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn(expiresAt - ttl10);

    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server responses.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse1));

    // Calling "authenticate()" the first time should result in a new, valid token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse1.getAccessToken());

    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse(vpcIamAccessTokenResponse2).setBodyDelay(2, TimeUnit.SECONDS));

    // Calling "authenticate()" again should result in re-using the cached access token,
    // but we should also trigger the acquisition of a new access token.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse1.getAccessToken());

    // Sleep to wait out the background refresh of our access token.
    Thread.sleep(3000);

    // Set our clock to be before the refresh window so we use the new access token that was
    // obtained above when we detected the need to do a background refresh,
    // but we also want to avoid trying to refresh again.
    PowerMockito.when(Clock.getCurrentTimeInSeconds()).thenReturn(expiresAt - ttl25);

    // Calling "authenticate()" a third time should result in the new access token being used.
    authenticator.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer " + vpcIamAccessTokenResponse2.getAccessToken());
  }

  @Test(expectedExceptions = ServiceResponseException.class)
  public void testAuthenticateErrorBadRequest() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return a 400 Bad Request response.
    server.enqueue(errorResponse(400, mockErrorResponseJson1));

    // Calling authenticate should result in an exception.
    authenticator.authenticate(requestBuilder);
  }

  @Test
  public void testAuthenticateResponseError1() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return an invalid JSON response body.
    server.enqueue(jsonResponse("{'}"));

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

  @Test
  public void testAuthenticateResponseError2() throws Throwable {
    VpcInstanceAuthenticator authenticator = new VpcInstanceAuthenticator.Builder()
        .iamProfileId(mockIamProfileId)
        .url(url)
        .build();

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Set mock server to return a valid instance identity token, but then
    // an invalid JSON response for the IAM token exchange.
    server.enqueue(jsonResponse(vpcInstanceIdentityTokenResponse));
    server.enqueue(jsonResponse("{'}"));

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
