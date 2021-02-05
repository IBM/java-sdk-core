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

import okhttp3.Request;

//
// This class contains an integration test that uses a live CP4D environment.
// This test is normally @Ignored to avoid trying to run this during automated builds because
// the CP4D test environment might not be available at all times.
//
// In order to test with a live CP4D server, create file "cp4dservicetest.env" in the project root.
// It should look like this:
//
// Required
// CP4DTEST1_AUTH_URL=<url> - e.g. https://cpd350-cpd-cpd350.apps.wml-kf-cluster.os.fyre.ibm.com/icp4d-api
// CP4DTEST1_AUTH_TYPE=cp4dService
// CP4DTEST1_SERVICE_BROKER_SECRET= - e.g. this value is obtained from the zen-service-broker-secret k8s secret
//
// Optional
// CP4DTEST1_USERNAME=<username> - the user name that will be encoded into the token
// CP4DTEST1_UID=<uid> - the uid that will be encoded into the token
// CP4DTEST1_DISPLAY_NAME= - the display name that will be encoded into the token
// CP4DTEST1_PERMISSIONS= - the comma-separated string of permissions that needs to be encoded into the token
// CP4DTEST1_EXPIRATION_TIME= - This is the time to live value of the token in minutes. Defaults to 5 minutes
//
// Then remove/comment-out the @Ignore annotation below and run the method as a junit test.
//
public class Cp4dServiceAuthenticatorLiveTest {

  @Ignore
  @Test
  public void testCp4dLiveTokenServer() {
    System.setProperty("IBM_CREDENTIALS_FILE", "cp4dservicetest.env");

    Authenticator auth1 = ConfigBasedAuthenticatorFactory.getAuthenticator("cp4dtest1");
    assertNotNull(auth1);

    Request.Builder requestBuilder;

    // Test the service authenticator
    requestBuilder = new Request.Builder().url("https://test.com");
    auth1.authenticate(requestBuilder);
    verifyAuthHeader(requestBuilder, "Bearer ");
  }

  // Verify the Authorization header in the specified request builder.
  private void verifyAuthHeader(Request.Builder builder, String expectedPrefix) {
    Request request = builder.build();
    String actualValue = request.header(HttpHeaders.AUTHORIZATION);
    assertNotNull(actualValue);
    assertTrue(actualValue.startsWith(expectedPrefix));
  }
}
