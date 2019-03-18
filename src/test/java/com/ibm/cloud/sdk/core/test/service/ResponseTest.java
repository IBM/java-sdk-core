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

import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ResponseTest extends BaseServiceUnitTest {
  private class TestModel extends GenericModel {
    String city;

    String getKey() {
      return city;
    }
  }

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    TestService() {
      super(SERVICE_NAME);
    }

    ServiceCall<TestModel> testMethod() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getEndPoint() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }

    ServiceCall<Void> testHeadMethod() {
      RequestBuilder builder = RequestBuilder.head(HttpUrl.parse(getEndPoint() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getVoid());
    }
  }

  private TestService service;
  private String testResponseKey = "city";
  private String testResponseValue = "Columbus";
  private String testResponseBody = "{\"" + testResponseKey + "\": \"" + testResponseValue + "\"}";

  // used for a specific test so we don't run into any weirdness with final, one-element, generic arrays
  private Response<TestModel> testResponseModel = null;

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.cloud.sdk.core.test.WatsonServiceTest#setUp()
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    service = new TestService();
    service.setUsernameAndPassword("", "");
    service.setEndPoint(getMockWebServerUrl());
  }

  /**
   * Test that all fields are populated when calling execute().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecute() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody));

    Response<TestModel> response = service.testMethod().execute();
    assertNotNull(response.getResult());
    assertEquals(testResponseValue, response.getResult().getKey());
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that all fields are populated when calling enqueue().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testEnqueue() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody));

    service.testMethod().enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        assertNotNull(response.getResult());
        assertEquals(testResponseValue, response.getResult().getKey());
        assertNotNull(response.getHeaders());
      }

      @Override
      public void onFailure(Exception e) { }
    });

    Thread.sleep(2000);
  }

  @Test
  public void testReactiveRequest() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody));

    Single<Response<TestModel>> observableRequest = service.testMethod().reactiveRequest();

    observableRequest
        .subscribeOn(Schedulers.single())
        .subscribe(new Consumer<Response<TestModel>>() {
          @Override
          public void accept(Response<TestModel> response) throws Exception {
            testResponseModel = response;
          }
        });

    // asynchronous, so test that we continued without a value yet
    assertNull(testResponseModel);
    Thread.sleep(2000);
    assertNotNull(testResponseModel);
    assertEquals(testResponseValue, testResponseModel.getResult().getKey());
    assertNotNull(testResponseModel.getHeaders());
  }

  /**
   * Test that headers are accessible from a HEAD method call using execute().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteForHead() throws InterruptedException {
    Headers rawHeaders = Headers.of("Content-Length", "472", "Content-Type", "application/json"
            , "Server", "Mock");
    com.ibm.cloud.sdk.core.http.Headers expectedHeaders =
            new com.ibm.cloud.sdk.core.http.Headers(rawHeaders);
    server.enqueue(new MockResponse().setHeaders(rawHeaders));

    Response<Void> response = service.testHeadMethod().execute();
    com.ibm.cloud.sdk.core.http.Headers actualHeaders = response.getHeaders();
    System.out.print(actualHeaders.equals(expectedHeaders));
    assertNull(response.getResult());
    assertNotNull(actualHeaders);
    // We can't just compare expectedHeaders.equals(actualHeaders) because of some underlying
    // whitespace weirdness in okhttp's Headers class.
    assertEquals(expectedHeaders.toString(), actualHeaders.toString());
  }
}
