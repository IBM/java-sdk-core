/**
 * (C) Copyright IBM Corp. 2021.
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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;

import okhttp3.Request;

//
// This class contains an integration test that uses the live IAM token service.
// This test is normally @Ignored to avoid trying to run this during automated builds.
//
// In order to run these tests, ceate file "iamtest.env" in the project root.
// It should look like this:
//
// IAMTEST1_AUTH_URL=<prod iam url>   e.g. https://iam.cloud.ibm.com
// IAMTEST1_AUTH_TYPE=iam
// IAMTEST1_APIKEY=<apikey>
//
// IAMTEST2_AUTH_URL=<test iam url>   e.g. https://iam.test.cloud.ibm.com
// IAMTEST2_AUTH_TYPE=iam
// IAMTEST2_APIKEY=<apikey>
// IAMTEST2_CLIENT_ID=bx
// IAMTEST2_CLIENT_SECRET=bx
//
// Then remove/comment-out the @Ignore annotation below and run the method as a junit test.
//
public class IamAuthenticatorLiveTest {

  @Ignore
  @Test
  public void testIamLiveTokenServer() {
    System.setProperty("IBM_CREDENTIALS_FILE", "iamtest.env");

    Authenticator auth1 = ConfigBasedAuthenticatorFactory.getAuthenticator("iamtest1");
    assertNotNull(auth1);

    Authenticator auth2 = ConfigBasedAuthenticatorFactory.getAuthenticator("iamtest2");
    assertNotNull(auth2);

    Request.Builder requestBuilder;

    // Perform a test using the "production" IAM token server.
    requestBuilder = new Request.Builder().url("https://test.com");
    auth1.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer ");

    // Perform a test using the "test" IAM token server.
    requestBuilder = new Request.Builder().url("https://test.com");
    auth2.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer ");

    // Test the retrieval of a refresh token from the "test" IAM token server.
    IamToken tokenResponse = ((IamAuthenticator) auth2).requestToken();
    assertNotNull(tokenResponse);
    String refreshToken = tokenResponse.getRefreshToken();
    assertNotNull(refreshToken);
    System.out.println("Refresh token: " + refreshToken);

    // This check is to ensure we get back a valid refresh token rather than something like "not_supported".
    assertTrue(refreshToken.length() > 20);
  }

  // Verify the Authorization header in the specified request builder.
  private void verifyAuthHeader(Request.Builder builder, String expectedPrefix) {
    Request request = builder.build();
    String actualValue = request.header(HttpHeaders.AUTHORIZATION);
    assertNotNull(actualValue);
    System.out.println("Authorization: " + actualValue);

    assertTrue(actualValue.startsWith(expectedPrefix));
  }
}
