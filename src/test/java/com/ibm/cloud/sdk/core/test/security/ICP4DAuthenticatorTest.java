package com.ibm.cloud.sdk.core.test.security;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.security.icp4d.ICP4DAuthenticator;
import com.ibm.cloud.sdk.security.icp4d.ICP4DConfig;
import com.ibm.cloud.sdk.security.icp4d.ICP4DTokenResponse;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;
import static org.junit.Assert.assertEquals;

public class ICP4DAuthenticatorTest extends BaseServiceUnitTest {

  private ICP4DTokenResponse validTokenData;
  private ICP4DTokenResponse expiredTokenData;
  private String url;
  private String testUsername = "test-username";
  private String testPassword = "test-password";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    validTokenData = loadFixture("src/test/resources/valid_icp4d_token.json", ICP4DTokenResponse.class);
    expiredTokenData = loadFixture("src/test/resources/expired_icp4d_token.json", ICP4DTokenResponse.class);
  }

  @Test
  public void testAuthenticateUserManagedToken() {
    String accessToken = "test-token";

    ICP4DConfig config = new ICP4DConfig.Builder()
        .url(url)
        .userManagedAccessToken(accessToken)
        .build();
    ICP4DAuthenticator authenticator = new ICP4DAuthenticator(config);
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    authenticator.authenticate(requestBuilder);
    Request request = requestBuilder.build();

    assertEquals("Bearer " + accessToken, request.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void testAuthenticateNewAndStoredToken() {
    server.enqueue(jsonResponse(validTokenData));

    ICP4DConfig config = new ICP4DConfig.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .disableSSLVerification(true)
        .build();
    ICP4DAuthenticator authenticator = new ICP4DAuthenticator(config);
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // should request new, valid token
    authenticator.authenticate(requestBuilder);
    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));

    // should just return the same token this time since we have a valid one stored
    Request.Builder newBuilder = request.newBuilder();
    authenticator.authenticate(newBuilder);
    Request newRequest = newBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), newRequest.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void testAuthenticationExpiredToken() {
    server.enqueue(jsonResponse(expiredTokenData));

    ICP4DConfig config = new ICP4DConfig.Builder()
        .url(url)
        .username(testUsername)
        .password(testPassword)
        .disableSSLVerification(true)
        .build();
    ICP4DAuthenticator authenticator = new ICP4DAuthenticator(config);
    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");

    // little hack to force the authenticator to store the expired token
    authenticator.authenticate(requestBuilder);

    // we should make a new API call since the token's expired, resulting in a valid one
    server.enqueue(jsonResponse(validTokenData));
    authenticator.authenticate(requestBuilder);
    Request request = requestBuilder.build();
    assertEquals("Bearer " + validTokenData.getAccessToken(), request.header(HttpHeaders.AUTHORIZATION));
  }
}
