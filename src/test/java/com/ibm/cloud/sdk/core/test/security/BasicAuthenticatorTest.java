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

import com.google.common.io.BaseEncoding;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;

import okhttp3.Request;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

public class BasicAuthenticatorTest {

  @Test
  public void testSuccess() {
    String username = "good-username";
    String password = "good-password";

    BasicAuthenticator auth = new BasicAuthenticator(username, password);
    assertEquals(Authenticator.AUTHTYPE_BASIC, auth.authenticationType());
    assertEquals(username, auth.getUsername());
    assertEquals(password, auth.getPassword());

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    auth.authenticate(requestBuilder);
    Request request = requestBuilder.build();

    String authHeader = request.header(HttpHeaders.AUTHORIZATION);
    assertNotNull(authHeader);
    assertEquals("Basic " + BaseEncoding.base64().encode((username + ":" + password).getBytes()), authHeader);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUsername() {
    new BasicAuthenticator("{bad-username}", "good-password");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    new BasicAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "{bad-username}");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    new BasicAuthenticator(props);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPassword() {
    new BasicAuthenticator("good-username", "{bad-password}");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingUsername() {
    new BasicAuthenticator(null, "good-password");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingPassword() {
    new BasicAuthenticator("good-username", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUsername() {
    new BasicAuthenticator("", "good-password");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyPassword() {
    new BasicAuthenticator("good-username", "");
  }

  @Test
  public void testGoodMapConfig() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator authenticator = new BasicAuthenticator(props);
    assertNotNull(authenticator);
    assertEquals(Authenticator.AUTHTYPE_BASIC, authenticator.authenticationType());
    assertEquals("good-username", authenticator.getUsername());
    assertEquals("good-password", authenticator.getPassword());
  }
}
