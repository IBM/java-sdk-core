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

package com.ibm.cloud.sdk.core.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_ENCODING;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class RequestTest extends BaseServiceUnitTest {
  private class TestModel extends GenericModel {
    String city;

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }
  }

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    TestService(Authenticator auth) {
      super(SERVICE_NAME, auth);
    }

    ServiceCall<TestModel> postTestModel() {
      final TestModel model = new TestModel();
      model.setCity("Columbus");
    
      final JsonObject contentJson = new JsonObject();
      contentJson.addProperty("city", model.getCity());
      RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      builder.bodyJson(contentJson).build();
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }
   
  }

  private TestService service;
  private String testResponseValue = "Columbus";
  private String testResponseBody1 = "{\"city\": \"Columbus\"}";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testRequestIncludeAuthGzipEnabled() throws Throwable {
    // setup the service & configure it with compression enabled
    IamAuthenticator authenticator = new IamAuthenticator("API_KEY");
    authenticator.setURL(getMockWebServerUrl());

    service = new TestService(authenticator);
    service.setServiceUrl(getMockWebServerUrl());
    service.enableGzipCompression(true);

    IamToken tokenData = null;
    try {
      tokenData = loadFixture("src/test/resources/iam_token.json", IamToken.class);
    } catch (Exception e) {
      fail(e.toString());
    }
    // the first response SHOULD just be the response from the call to authenticate()
    // so queue that first
    server.enqueue(jsonResponse(tokenData));
    server.enqueue(new MockResponse().setBody(testResponseBody1));
    // Validate the response
    Response<TestModel> response = service.postTestModel().execute();
    assertNotNull(response.getResult());
    assertEquals(testResponseValue, response.getResult().getCity());
    // Validate the first request wasn't compressed. This should be the call to authenticate()
    String expectedAuthBody = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=API_KEY&response_type=cloud_iam";
    RecordedRequest request = server.takeRequest();
    assertEquals(request.getMethod(), "POST");
    assertEquals(expectedAuthBody, request.getBody().readUtf8());
    assertNull(request.getHeader(CONTENT_ENCODING));
    // Validate the next request was compressed, which should be the call to the service
    String expectedOperationBody = "{\"city\": \"Columbus\"}";
    request = server.takeRequest();
    assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
    assertFalse(expectedOperationBody == request.getBody().readUtf8());
    assertEquals(request.getMethod(), "POST");
  }
}
