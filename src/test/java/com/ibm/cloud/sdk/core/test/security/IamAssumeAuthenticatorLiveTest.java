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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

import okhttp3.Request;

//
// This class contains an integration test that uses the live IAM token service.
// This test is normally @Ignored to avoid trying to run this during automated builds.
//
// In order to run these tests, create file "iamassume.env" in the project root.
// It should look like this:
//
// IAMASSUME1_AUTH_URL=<iam url>   e.g. https://iam.cloud.ibm.com
// IAMASSUME1_AUTH_TYPE=iamAssume
// IAMASSUME1_APIKEY=<apikey>
// IAMASSUME1_IAM_PROFILE_ID=<id of trusted profile resource>
//
// Then remove/comment-out the @Ignore annotation below and run the method as a junit test.
//
public class IamAssumeAuthenticatorLiveTest {

  @Ignore
  @Test
  public void testIamLiveTokenServer() {
    System.setProperty("IBM_CREDENTIALS_FILE", "iamassume.env");

    Authenticator auth1 = ConfigBasedAuthenticatorFactory.getAuthenticator("iamassume1");
    assertNotNull(auth1);

    Request.Builder requestBuilder;

    // Perform a test using the "production" IAM token server.
    requestBuilder = new Request.Builder().url("https://test.com");
    auth1.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer ");
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
