/**
 * (C) Copyright IBM Corp. 2015, 2022.
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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class BasicAuthenticatorTest {

  @Test
  public void testSuccess() {
    String username = "good-username";
    String password = "good-password";

    BasicAuthenticator auth = new BasicAuthenticator.Builder()
        .username(username)
        .password(password)
        .build();
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


  //
  // Tests involving the Builder class and fromConfiguration() method.
  //

  @Test()
  public void testBuilderSuccess() {
    BasicAuthenticator auth = new BasicAuthenticator.Builder()
      .username("good-user")
      .password("good-password")
      .build();
    assertNotNull(auth);
    assertEquals(auth.getUsername(), "good-user");
    assertEquals(auth.getPassword(), "good-password");

    BasicAuthenticator auth2 = auth.newBuilder().build();
    assertNotNull(auth2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingUsername() {
    new BasicAuthenticator.Builder()
      .username(null)
      .password("good-password")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyUsername() {
    new BasicAuthenticator.Builder()
      .username("")
      .password("good-password")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInvalidUsername() {
    new BasicAuthenticator.Builder()
      .username("{bad-username}")
      .password("good-password")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingPassword() {
    new BasicAuthenticator.Builder()
      .username("good-username")
      .password(null)
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyPassword() {
    new BasicAuthenticator.Builder()
      .username("good-username")
      .password("")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInvalidPassword() {
    new BasicAuthenticator.Builder()
      .username("good-username")
      .password("{bad-password}")
      .build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, null);
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigInvalidUsername() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "{bad-username}");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigMissingPassword() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, null);
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigEmptyPassword() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "");
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConfigInvalidPassword() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "{bad-password}");
    BasicAuthenticator.fromConfiguration(props);
  }

  @Test
  public void testConfigGoodConfig() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator authenticator = BasicAuthenticator.fromConfiguration(props);
    assertNotNull(authenticator);
    assertEquals(Authenticator.AUTHTYPE_BASIC, authenticator.authenticationType());
    assertEquals("good-username", authenticator.getUsername());
    assertEquals("good-password", authenticator.getPassword());
  }


  //
  // Tests involving the deprecated ctors.
  //

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorInvalidUsername() {
    new BasicAuthenticator("{bad-username}", "good-password");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    new BasicAuthenticator(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorInvalidUsernameMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "{bad-username}");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    new BasicAuthenticator(props);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorInvalidPassword() {
    new BasicAuthenticator("good-username", "{bad-password}");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingUsername() {
    new BasicAuthenticator(null, "good-password");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorMissingPassword() {
    new BasicAuthenticator("good-username", null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyUsername() {
    new BasicAuthenticator("", "good-password");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCtorEmptyPassword() {
    new BasicAuthenticator("good-username", "");
  }

  @Test
  public void testCtorGoodConfigMap() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator authenticator = new BasicAuthenticator(props);
    assertNotNull(authenticator);
    assertEquals(Authenticator.AUTHTYPE_BASIC, authenticator.authenticationType());
    assertEquals("good-username", authenticator.getUsername());
    assertEquals("good-password", authenticator.getPassword());
  }

  @Test
  public void testSingleAuthHeader() {
    Map<String, String> props = new HashMap<>();
    props.put(Authenticator.PROPNAME_USERNAME, "good-username");
    props.put(Authenticator.PROPNAME_PASSWORD, "good-password");
    BasicAuthenticator auth = new BasicAuthenticator(props);

    Request.Builder requestBuilder = new Request.Builder().url("https://test.com");
    auth.authenticate(requestBuilder);
    // call authenticate twice on the same request
    auth.authenticate(requestBuilder);
    Request request = requestBuilder.build();

    List<String> authHeaders = request.headers(HttpHeaders.AUTHORIZATION);
    assertEquals(authHeaders.size(), 1);
  }
}
