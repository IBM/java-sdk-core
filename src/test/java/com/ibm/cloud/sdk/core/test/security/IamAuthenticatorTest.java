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
import com.ibm.cloud.sdk.core.security.AuthenticatorBase;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.mockwebserver.RecordedRequest;

public class IamAuthenticatorTest extends BaseServiceUnitTest {

  private IamToken validTokenData;
  private IamToken expiredTokenData;
  private String url;

  private static final String API_KEY = "123456789";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    expiredTokenData = loadFixture("src/test/resources/expired_iam_token.json", IamToken.class);
    validTokenData = loadFixture("src/test/resources/valid_iam_token.json", IamToken.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingApiKey() {
    new IamAuthenticator((String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyApiKey() {
    new IamAuthenticator("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingApiKeyMap() {
    Map<String, String> props = new HashMap<>();
    new IamAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyApiKeyMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, "");
    new IamAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingClientId() {
    new IamAuthenticator(API_KEY, "url", null, "clientSecret", false, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyClientId() {
    new IamAuthenticator(API_KEY, "url", "", "clientSecret", false, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingClientSecret() {
    new IamAuthenticator(API_KEY, "url", "clientId", null, false, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyClientSecret() {
    new IamAuthenticator(API_KEY, "url", "clientId", "", false, null);
  }

  @Test
  public void testCorrectConfig() {
    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, "url", "clientId", "clientSecret", false, null);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertFalse(authenticator.getDisableSSLVerification());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
  }

  @Test
  public void testCorrectConfigMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_APIKEY, API_KEY);
    props.put(Authenticator.PROPNAME_URL, "url");
    props.put(Authenticator.PROPNAME_CLIENT_ID, "clientId");
    props.put(Authenticator.PROPNAME_CLIENT_SECRET, "clientSecret");
    props.put(Authenticator.PROPNAME_DISABLE_SSL, "true");

    IamAuthenticator authenticator = new IamAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertTrue(authenticator.getDisableSSLVerification());
    assertEquals("clientId", authenticator.getClientId());
    assertEquals("clientSecret", authenticator.getClientSecret());
    assertEquals(API_KEY, authenticator.getApiKey());
    assertEquals("url", authenticator.getURL());
  }

  @Test
  public void testCorrectConfigMap2() {
    Map<String, String> props = new HashMap<>();
    props.put("IAM_APIKEY", API_KEY);

    IamAuthenticator authenticator = new IamAuthenticator(props);
    assertEquals(Authenticator.AUTHTYPE_IAM, authenticator.authenticationType());
    assertFalse(authenticator.getDisableSSLVerification());
    assertEquals(API_KEY, authenticator.getApiKey());
  }

  @Test
  public void testDisableSSLVerification() {
    // Test using the 1-arg ctor and setter.
    IamAuthenticator auth = new IamAuthenticator(API_KEY);
    assertFalse(auth.getDisableSSLVerification());
    auth.setDisableSSLVerification(true);
    assertTrue(auth.getDisableSSLVerification());

    // Test using the full ctor.
    auth = new IamAuthenticator(API_KEY, "url", null, null, true, null);
    assertTrue(auth.getDisableSSLVerification());
  }

  @Test
  public void testAuthenticateNewAndStoredToken() throws Throwable {
    server.enqueue(jsonResponse(validTokenData));

    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, url, null, null, true, null);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);

    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));

    // Now make sure the token server request did not contain an Authorization header,
    // since we didn't set clientId/clientSecret.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertNull(actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Authenticator should just return the same token this time since we have a valid one stored.
    Request.Builder newBuilder = request.newBuilder();
    authenticator.authenticate(newBuilder);

    Request newRequest = newBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), newRequest.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(expiredTokenData));

    IamAuthenticator authenticator = new IamAuthenticator(API_KEY);
    authenticator.setURL(url);

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

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, url, null, null, false, headers);

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

  @Test
  public void testClientIdSecret() throws Throwable {
    server.enqueue(jsonResponse(validTokenData));

    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, url, "clientId", "clientSecret", false, null);
    String expectedIAMAuthHeader = AuthenticatorBase.constructBasicAuthHeader("clientId", "clientSecret");

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // Authenticator should request new, valid token.
    authenticator.authenticate(requestBuilder);

    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));

    // Now make sure the token server request contained the correct Authorization header.
    RecordedRequest tokenServerRequest = server.takeRequest();
    assertNotNull(tokenServerRequest);
    assertNotNull(tokenServerRequest.getHeaders());
    Headers actualHeaders = tokenServerRequest.getHeaders();
    assertEquals(expectedIAMAuthHeader, actualHeaders.get(HttpHeaders.AUTHORIZATION));

    // Authenticator should just return the same token this time since we have a valid one stored.
    Request.Builder newBuilder = request.newBuilder();
    authenticator.authenticate(newBuilder);

    Request newRequest = newBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), newRequest.header(HttpHeaders.AUTHORIZATION));
  }
}
