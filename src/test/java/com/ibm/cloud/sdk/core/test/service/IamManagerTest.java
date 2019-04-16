/**
 * Copyright 2018 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.test.service;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.cloud.sdk.core.service.security.IamToken;
import com.ibm.cloud.sdk.core.service.security.IamTokenManager;

import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class IamManagerTest extends BaseServiceUnitTest {

  private IamToken expiredTokenData;
  private IamToken validTokenData;
  private String url;

  private static final String ACCESS_TOKEN = "abcd-1234";
  private static final String API_KEY = "123456789";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    expiredTokenData = loadFixture("src/test/resources/expired_iam_token.json", IamToken.class);
    validTokenData = loadFixture("src/test/resources/valid_iam_token.json", IamToken.class);
  }

  @Test
  public void testAuthorizationHeader() {
    // Make sure the default header value is correct.
    String header1 = IamTokenManager.getAuthorizationHeaderValue();
    assertEquals(header1, "Basic Yng6Yng=");

    // Now make sure different clientid/secret combinations yield different header values

    IamTokenManager.setIamClientId("myuser");
    IamTokenManager.setIamSecret("mysecret");
    String header2 = IamTokenManager.getAuthorizationHeaderValue();
    assertNotEquals(header1, header2);

    IamTokenManager.setIamClientId("123j10iii38918-afde3");
    IamTokenManager.setIamSecret("aU4RyzIZdFgZWxEroo1");
    String header3 = IamTokenManager.getAuthorizationHeaderValue();
    assertNotEquals(header1, header3);
    assertNotEquals(header2, header3);
  }

  /**
   * Tests that if a user passes in an access token during initial IAM setup, that access token is passed back
   * during later retrieval.
   */
  @Test
  public void getUserManagedTokenFromConstructor() {
    IamOptions options = new IamOptions.Builder()
        .accessToken(ACCESS_TOKEN)
        .url(url)
        .build();
    IamTokenManager manager = new IamTokenManager(options);

    String token = manager.getToken();
    assertEquals(ACCESS_TOKEN, token);
  }

  /**
   * Tests that if only an API key is stored, the user can get back a valid access token.
   */
  @Test
  public void getTokenFromApiKey() throws InterruptedException {
    server.enqueue(jsonResponse(validTokenData));

    IamOptions options = new IamOptions.Builder()
        .apiKey(API_KEY)
        .url(url)
        .build();
    IamTokenManager manager = new IamTokenManager(options);

    String token = manager.getToken();
    assertEquals(validTokenData.getAccessToken(), token);
  }

  /**
   * Tests that if the stored access token is expired, it can be refreshed properly.
   */
  @Test
  public void getTokenAfterRefresh() {
    server.enqueue(jsonResponse(expiredTokenData));

    IamOptions options = new IamOptions.Builder()
        .apiKey(API_KEY)
        .url(url)
        .build();
    IamTokenManager manager = new IamTokenManager(options);

    // setting expired token
    manager.getToken();

    // getting valid token
    server.enqueue(jsonResponse(validTokenData));
    String newToken = manager.getToken();

    assertEquals(validTokenData.getAccessToken(), newToken);
  }
}
