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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.ConfigBasedAuthenticatorFactory;

import okhttp3.Request;

//
// This class contains an integration test that uses the live MCSP v2 token service.
// This test is normally @Ignored to avoid trying to run this during automated builds.
//
// In order to run these tests, create file "mcspv2test.env" in the project root.
// It should look like this:
//
// required properties:
//
// MCSPV2TEST1_AUTH_URL=<url>   e.g. https://account-iam.platform.dev.saas.ibm.com
// MCSPV2TEST1_AUTH_TYPE=mcspv2
// MCSPV2TEST1_APIKEY=<apikey>
// MCSPV2TEST1_SCOPE_COLLECTION_TYPE=accounts  (use any valid collection type value)
// MCSPV2TEST1_SCOPE_ID=global_account         (use any valid scope id)
//
// optional properties:
//
// MCSPV2TEST1_INCLUDE_BUILTIN_ACTIONS=true|false
// MCSPV2TEST1_INCLUDE_CUSTOM_ACTIONS=true|false
// MCSPV2TEST1_INCLUDE_ROLES=true|false
// MCSPV2TEST1_PREFIX_ROLES=true|false
// MCSPV2TEST1_CALLER_EXT_CLAIM={"productID":"prod123"}
//
// Then remove/comment-out the @Ignore annotation below and run the method as a TestNG test in eclipse,
// or via command line:
//    mvn test -Dtest=MCSPV2AuthenticatorLiveTest -Djava.util.logging.config.file=debug-logging.properties
//
public class MCSPV2AuthenticatorLiveTest {

  @Ignore
  @Test
  public void testMCSPLiveTokenServer() {
    System.setProperty("IBM_CREDENTIALS_FILE", "mcspv2test.env");

    Authenticator auth1 = ConfigBasedAuthenticatorFactory.getAuthenticator("mcspv2test1");
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
