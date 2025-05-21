/**
 * (C) Copyright IBM Corp. 2025.
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.MCSPV2Authenticator;
import com.ibm.cloud.sdk.core.security.MCSPV2TokenResponse;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class MCSPV2AuthenticatorTest extends BaseServiceUnitTest {

    // Token with issued-at time of 1699026536 and expiration time of 1699033736
    private MCSPV2TokenResponse tokenData;

    // Token with issued-at time of 1699037852 and expiration time of 1699045052
    private MCSPV2TokenResponse refreshedTokenData;

    // The mock server's URL.
    private String url;

    private static final String API_KEY = "<my-api-key>";
    private static final String AUTH_URL = "https://mcspv2.token-exchange.com";
    private static final String SCOPE_COLLECTION_TYPE = "accounts";
    private static final String SCOPE_ID = "global_accounts";

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        url = getMockWebServerUrl();
        tokenData = loadFixture("src/test/resources/mcspv2_token.json", MCSPV2TokenResponse.class);
        refreshedTokenData = loadFixture("src/test/resources/refreshed_mcspv2_token.json", MCSPV2TokenResponse.class);
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
        new MCSPV2Authenticator.Builder()
            .apikey(null)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyApiKey() {
        new MCSPV2Authenticator.Builder()
            .apikey("")
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingURL() {
        new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(null)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingScopeCollectionType() {
        new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(null)
            .scopeId(SCOPE_ID)
            .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingScopeId() {
        new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(null)
            .build();
    }

    @Test
    public void testBuilderDefaultConfig() {
        // Create an authenticator with only required properties,
        // then verify that each optional property contains its default value.
        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();
        assertNotNull(authenticator);
        assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_MCSPV2);
        assertEquals(authenticator.getApiKey(), API_KEY);
        assertEquals(authenticator.getURL(), AUTH_URL);
        assertEquals(authenticator.getScopeCollectionType(), SCOPE_COLLECTION_TYPE);
        assertEquals(authenticator.getScopeId(), SCOPE_ID);
        assertFalse(authenticator.includeBuiltinActions());
        assertFalse(authenticator.includeCustomActions());
        assertTrue(authenticator.includeRoles());
        assertFalse(authenticator.prefixRoles());
        assertNull(authenticator.getCallerExtClaim());
        assertFalse(authenticator.getDisableSSLVerification());
        assertNull(authenticator.getHeaders());
        assertNull(authenticator.getProxy());
        assertNull(authenticator.getProxyAuthenticator());
    }

    @Test
    public void testBuilderCorrectConfig() {
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("header1", "value1");
        expectedHeaders.put("header2", "value2");

        Map<String, String> expectedCallerExtClaim = new HashMap<>();
        expectedCallerExtClaim.put("productID", "my-product-123");
        expectedCallerExtClaim.put("serviceID", "my-serviceid-123");

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .includeBuiltinActions(true)
            .includeCustomActions(true)
            .includeRoles(false)
            .prefixRoles(true)
            .callerExtClaim(expectedCallerExtClaim)
            .disableSSLVerification(true)
            .headers(expectedHeaders)
            .proxy(null)
            .proxyAuthenticator(null)
            .build();
        assertNotNull(authenticator);
        assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_MCSPV2);
        assertEquals(authenticator.getApiKey(), API_KEY);
        assertEquals(authenticator.getURL(), AUTH_URL);
        assertEquals(authenticator.getScopeCollectionType(), SCOPE_COLLECTION_TYPE);
        assertEquals(authenticator.getScopeId(), SCOPE_ID);
        assertTrue(authenticator.includeBuiltinActions());
        assertTrue(authenticator.includeCustomActions());
        assertFalse(authenticator.includeRoles());
        assertTrue(authenticator.prefixRoles());
        assertEquals(authenticator.getCallerExtClaim(), expectedCallerExtClaim);
        assertTrue(authenticator.getDisableSSLVerification());
        assertEquals(authenticator.getHeaders(), expectedHeaders);
        assertNull(authenticator.getProxy());
        assertNull(authenticator.getProxyAuthenticator());

        // Next, create a new builder from the existing authenticator, set the "includeRoles" flag
        // and build a new authenticator from the builder, then verify that the new authenticator
        // is the same as the prior one except for the includeRoles flag.
        authenticator = authenticator.newBuilder()
            .includeRoles(true)
            .build();
        assertNotNull(authenticator);
        assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_MCSPV2);
        assertEquals(authenticator.getApiKey(), API_KEY);
        assertEquals(authenticator.getURL(), AUTH_URL);
        assertEquals(authenticator.getScopeCollectionType(), SCOPE_COLLECTION_TYPE);
        assertEquals(authenticator.getScopeId(), SCOPE_ID);
        assertTrue(authenticator.includeBuiltinActions());
        assertTrue(authenticator.includeCustomActions());
        assertTrue(authenticator.includeRoles());
        assertTrue(authenticator.prefixRoles());
        assertEquals(authenticator.getCallerExtClaim(), expectedCallerExtClaim);
        assertTrue(authenticator.getDisableSSLVerification());
        assertEquals(authenticator.getHeaders(), expectedHeaders);
        assertNull(authenticator.getProxy());
        assertNull(authenticator.getProxyAuthenticator());
    }

    @Test
    public void testConfigCorrectConfig() {
        // Create a "callerExtClaim" map and then serialize it to a string.
        Map<String, String> expectedCallerExtClaim = new HashMap<>();
        expectedCallerExtClaim.put("productID", "my-prod-123");
        Gson gson = GsonSingleton.getGsonWithoutPrettyPrinting();
        String callerExtClaimStr = gson.toJson(expectedCallerExtClaim);

        Map<String, String> props = new HashMap<>();
        props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
        props.put(Authenticator.PROPNAME_URL, AUTH_URL);
        props.put(Authenticator.PROPNAME_SCOPE_COLLECTION_TYPE, SCOPE_COLLECTION_TYPE);
        props.put(Authenticator.PROPNAME_SCOPE_ID, SCOPE_ID);
        props.put(Authenticator.PROPNAME_INCLUDE_BUILTIN_ACTIONS, "true");
        props.put(Authenticator.PROPNAME_INCLUDE_ROLES, "false");
        props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");
        props.put(Authenticator.PROPNAME_CALLER_EXT_CLAIM, callerExtClaimStr);

        MCSPV2Authenticator authenticator = MCSPV2Authenticator.fromConfiguration(props);
        assertEquals(authenticator.authenticationType(), Authenticator.AUTHTYPE_MCSPV2);
        assertEquals(authenticator.getApiKey(), API_KEY);
        assertEquals(authenticator.getURL(), AUTH_URL);
        assertEquals(authenticator.getScopeCollectionType(), SCOPE_COLLECTION_TYPE);
        assertEquals(authenticator.getScopeId(), SCOPE_ID);
        assertTrue(authenticator.includeBuiltinActions());
        assertFalse(authenticator.includeRoles());
        assertTrue(authenticator.getDisableSSLVerification());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConfigIncorrectConfig1() {
        Map<String, String> props = new HashMap<>();
        props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
        props.put(Authenticator.PROPNAME_URL, AUTH_URL);
        props.put(Authenticator.PROPNAME_SCOPE_COLLECTION_TYPE, SCOPE_COLLECTION_TYPE);
        props.put(Authenticator.PROPNAME_SCOPE_ID, SCOPE_ID);
        props.put(Authenticator.PROPNAME_INCLUDE_BUILTIN_ACTIONS, "not_a_boolean");

        MCSPV2Authenticator.fromConfiguration(props);
    }

    @Test(expectedExceptions = JsonSyntaxException.class)
    public void testConfigIncorrectConfig2() {
        Map<String, String> props = new HashMap<>();
        props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
        props.put(Authenticator.PROPNAME_URL, AUTH_URL);
        props.put(Authenticator.PROPNAME_SCOPE_COLLECTION_TYPE, SCOPE_COLLECTION_TYPE);
        props.put(Authenticator.PROPNAME_SCOPE_ID, SCOPE_ID);
        props.put(Authenticator.PROPNAME_CALLER_EXT_CLAIM, "{ invalid_json!!! }");

        MCSPV2Authenticator.fromConfiguration(props);
    }

    @Test
    public void testDisableSSLVerification() {
        MCSPV2Authenticator auth = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();
        assertFalse(auth.getDisableSSLVerification());

        auth = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(AUTH_URL)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .disableSSLVerification(true)
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

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
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
        authenticator = authenticator.newBuilder().client(client).build();
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

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();

        Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

        // This will bootstrap the test by forcing the Authenticator to store the expired token
        // set above in the mock server.
        authenticator.authenticate(requestBuilder);

        // Authenticator should detect the expiration and request a new access token when we call
        // authenticate() again.
        server.enqueue(jsonResponse(refreshedTokenData));
        authenticator.authenticate(requestBuilder);
        verifyAuthHeader(requestBuilder, "Bearer " + refreshedTokenData.getToken());
    }

    @Test
    public void testAuthenticationBackgroundTokenRefresh() throws InterruptedException {
        server.enqueue(jsonResponse(tokenData));

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .build();

        Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

        // This will bootstrap the test by forcing the Authenticator to store the token needing to be
        // refreshed, which was set above in the mock server.
        authenticator.authenticate(requestBuilder);

        // Mock current time to put us in the "refresh window" where the token is not expired but still
        // needs to be refreshed. This time is within the refresh window associated with the token in mcspv2_token.json
        // (i.e. the token contains iat=1747769783, exp=iat+7200=1747776983).
        clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 1747775583);

        // Authenticator should detect the need to refresh and request a new access token IN THE BACKGROUND
        // when we call authenticate() again. The immediate response should be the token which was already stored, since
        // it's not yet expired.
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

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("header1", "value1");
        expectedHeaders.put("header2", "value2");
        expectedHeaders.put("Host", "mcsp.cloud.ibm.com:81");
        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
            .headers(expectedHeaders)
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
        assertEquals(actualHeaders.get("header1"), "value1");
        assertEquals(actualHeaders.get("header2"), "value2");
        assertEquals(actualHeaders.get("Host"), "mcsp.cloud.ibm.com:81");
        assertTrue(actualHeaders.get(HttpHeaders.USER_AGENT).startsWith("ibm-java-sdk-core/mcspv2-authenticator"));

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

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
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
        assertEquals(body, expectedBody);
    }

    // @Ignore
    @Test(expectedExceptions = ServiceResponseException.class)
    public void testApiErrorBadRequest() throws Throwable {
        server.enqueue(errorResponse(400));

        // Mock current time to ensure the token is valid.
        clockMock.when(() -> Clock.getCurrentTimeInSeconds()).thenReturn((long) 100);

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
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

        MCSPV2Authenticator authenticator = new MCSPV2Authenticator.Builder()
            .apikey(API_KEY)
            .url(url)
            .scopeCollectionType(SCOPE_COLLECTION_TYPE)
            .scopeId(SCOPE_ID)
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
