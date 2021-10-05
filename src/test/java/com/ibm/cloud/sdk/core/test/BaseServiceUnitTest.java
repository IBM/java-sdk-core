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

package com.ibm.cloud.sdk.core.test;

import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterMethod;
import org.powermock.modules.testng.PowerMockTestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BaseServiceUnitTest extends PowerMockTestCase {
  private static final Gson GSON = GsonSingleton.getGson();

  /** The server. */
  protected MockWebServer server;

  /**
   * Setups and starts the mock server.
   *
   * @throws Exception the exception
   */
  public void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  /**
   * Tear down.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @AfterMethod
  public void tearDown() throws IOException {
    server.shutdown();
  }

  /**
   * Gets the mock web server url.
   *
   * @return the server url
   */
  protected String getMockWebServerUrl() {
    return StringUtils.chop(server.url("/").toString());
  }

  /**
   * Create a MockResponse with JSON content type and the object serialized to JSON as body.
   *
   * @param body the body
   * @return the mock response
   */
  protected static MockResponse jsonResponse(Object body) {
    return new MockResponse().addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON).setBody(GSON.toJson(body));
  }

  /**
   * Create a MockResponse with JSON content type and the object serialized to JSON as body.
   *
   * @param body the body
   * @return the mock response
   */
  protected static MockResponse errorResponse(int statusCode) {
    return new MockResponse().setResponseCode(statusCode);
  }

  /**
   * Parse the form-url-encoded body in the specified request into a map of key/value pairs
   * to make it easy to validate things.
   * @param request the RecordedRequest to retrieve the request body from
   * @return a map containing the form parameters found in the request body
   */
  protected Map<String, String> getFormBodyAsMap(RecordedRequest request) {

    // Retrieve the request body as a string.
    // It should look like a query string (example: "grant_type=foo&cr_token=bar..."
    String formBody = request.getBody().readUtf8();

    // Next, parse the request body contents into its individual form parameters.
    // Because the request body is form-url-encoded, it looks just like a query string,
    // so we'll use okhttp's HttpUrl class to help us parse/decode it.
    // Note: in order to pass a well-formed URL string to the HttpUrl.parse() function,
    // we'll just tack on a fake request URL before the form body string.
    HttpUrl url = HttpUrl.parse("https://fakehost.com/?" + formBody);
    Set<String> paramNames = url.queryParameterNames();

    // Finally, walk through the set of parsed "query" params and add each one to the map.
    Map<String, String> formProps = new HashMap<>();
    for (String name : paramNames) {
      String value = url.queryParameter(name);
      if (value != null) {
        formProps.put(name, value);
      }
    }
    return formProps;
  }

  // Verify the Authorization header in the specified request builder.
  protected void verifyAuthHeader(Request.Builder builder, String expectedPrefix) {
    Request request = builder.build();
    List<String> authHeaders = request.headers(HttpHeaders.AUTHORIZATION);
    assertEquals(authHeaders.size(), 1);
    String actualValue = authHeaders.get(0);
    assertNotNull(actualValue);

    assertTrue(actualValue.startsWith(expectedPrefix));
  }
}
