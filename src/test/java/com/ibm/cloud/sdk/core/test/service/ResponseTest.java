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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ibm.cloud.sdk.core.util.Clock;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
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

public class ResponseTest extends BaseServiceUnitTest {
  private class TestModel extends GenericModel {
    String city;

    String getCity() {
      return city;
    }
  }

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    TestService(Authenticator auth) {
      super(SERVICE_NAME, auth);
    }

    ServiceCall<TestModel> getTestModel() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }

    ServiceCall<TestModel> getTestModel2() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<TestModel> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<TestModel>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<Void> headMethod() {
      RequestBuilder builder = RequestBuilder.head(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getVoid());
    }

    ServiceCall<String> getString() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<String> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<String>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<String>> getListString() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<String>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<String>>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<Long> getLong() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<Long> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<Long>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<Long>> getListLong() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<Long>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<Long>>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<TestModel>> getListTestModel() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<TestModel>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<TestModel>>(){}.getType());
      return createServiceCall(builder.build(), responseConverter);
    }
  }

  private TestService service;
  private String testResponseValue = "Columbus";
  private String testResponseBody1 = "{\"city\": \"Columbus\"}";
  private String testResponseBody2 = "[\"string1\",\"string2\",\"string3\"]";
  private String testResponseBody3 = "[44,33,74]";
  private String testResponseBody4 = "[{\"city\":\"Austin\"},{\"city\":\"Georgetown\"},{\"city\":\"Cedar Park\"}]";
  private String testResponseBody5 = "\"string response\"";
  private String testResponseBody6 = "443374";

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
    service = new TestService(new NoAuthAuthenticator());
    service.setServiceUrl(getMockWebServerUrl());
  }

  /**
   * Test that all fields are populated when calling execute().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteTestModel() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody1));

    Response<TestModel> response = service.getTestModel().execute();
    assertNotNull(response.getResult());
    assertEquals(testResponseValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that all fields are populated when calling enqueue().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testEnqueue() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody1));

    service.getTestModel().enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        assertNotNull(response.getResult());
        assertEquals(testResponseValue, response.getResult().getCity());
        assertNotNull(response.getHeaders());
      }

      @Override
      public void onFailure(Exception e) { }
    });

    Thread.sleep(2000);
  }

  @Test
  public void testReactiveRequest() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody1));

    Single<Response<TestModel>> observableRequest = service.getTestModel().reactiveRequest();

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
    assertEquals(testResponseValue, testResponseModel.getResult().getCity());
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

    Response<Void> response = service.headMethod().execute();
    com.ibm.cloud.sdk.core.http.Headers actualHeaders = response.getHeaders();
    System.out.print(actualHeaders.equals(expectedHeaders));
    assertNull(response.getResult());
    assertNotNull(actualHeaders);
    // We can't just compare expectedHeaders.equals(actualHeaders) because of some underlying
    // whitespace weirdness in okhttp's Headers class.
    assertEquals(expectedHeaders.toString(), actualHeaders.toString());
  }

  /**
   * Test that all fields are populated when calling execute().
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteTestModel2() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody1));

    Response<TestModel> response = service.getTestModel2().execute();
    assertNotNull(response.getResult());
    assertEquals(testResponseValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that a list of strings response can be deserialized correctly.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteString() {
    server.enqueue(new MockResponse().setBody(testResponseBody5));

    Response<String> response = service.getString().execute();
    String result = response.getResult();
    assertNotNull(result);
    assertEquals("string response", result);
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that a list of strings response can be deserialized correctly.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteListString() {
    server.enqueue(new MockResponse().setBody(testResponseBody2));

    Response<List<String>> response = service.getListString().execute();
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", "string2", "string3"), result);
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that a list of strings response can be deserialized correctly.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteLong() {
    server.enqueue(new MockResponse().setBody(testResponseBody6));

    Response<Long> response = service.getLong().execute();
    Long result = response.getResult();
    assertNotNull(result);
    assertEquals(Long.valueOf(443374), result);
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that a list of longs response can be deserialized correctly.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteListLong() {
    server.enqueue(new MockResponse().setBody(testResponseBody3));

    Response<List<Long>> response = service.getListLong().execute();
    List<Long> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);

    List<Long> expectedResult = new ArrayList<>();
    expectedResult.add(Long.valueOf(44));
    expectedResult.add(Long.valueOf(33));
    expectedResult.add(Long.valueOf(74));

    assertEquals(expectedResult, result);
    assertNotNull(response.getHeaders());
  }

  /**
   * Test that list of TestModels response can be deserialized correctly.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testExecuteListTestModel() {
    server.enqueue(new MockResponse().setBody(testResponseBody4));

    Response<List<TestModel>> response = service.getListTestModel().execute();
    List<TestModel> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    List<String> actualCities = new ArrayList<>();
    for (TestModel obj : result) {
      actualCities.add(obj.getCity());
    }
    assertEquals(Arrays.asList("Austin", "Georgetown", "Cedar Park"), actualCities);
    assertNotNull(response.getHeaders());
  }

  /**
   * Test getting the status code from a response.
   */
  @Test
  public void testResponseCode() {
    server.enqueue(new MockResponse().setResponseCode(204));
    Response<Void> response = service.headMethod().execute();
    assertEquals("The response status code should be 204.", 204, response.getStatusCode());
  }

  /**
   * Test getting the status line message from a response.
   */
  @Test
  public void testResponseMessage() {
    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 No Content"));
    Response<Void> response = service.headMethod().execute();
    assertEquals("The response status message should be 'No Content'.", "No Content", response.getStatusMessage());
  }

  /**
   * Test canceling a service call by mimicking setting a timeout and canceling if the call exceeds that value.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testRequestCancel() throws InterruptedException {
    server.enqueue(new MockResponse().setBody(testResponseBody1).setBodyDelay(5000, TimeUnit.MILLISECONDS));

    // time to consider timeout (in ms)
    long timeoutThreshold = 3000;
    final boolean[] hasCallCompleted = {false};
    final boolean[] callWasCanceled = {false};

    ServiceCall<TestModel> testCall = service.getTestModel();
    long startTime = Clock.getCurrentTimeInMillis();
    testCall.enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        hasCallCompleted[0] = true;
        System.out.println("We got a response!");
      }

      @Override
      public void onFailure(Exception e) {
        callWasCanceled[0] = true;
        System.out.println("The request failed :(");
      }
    });

    // keep waiting for the call to complete while we're within the timeout bounds
    while (!hasCallCompleted[0] && (Clock.getCurrentTimeInMillis() - startTime < timeoutThreshold)) {
      Thread.sleep(500);
    }

    // if we timed out and it's STILL not complete, we'll just cancel the call
    if (!hasCallCompleted[0]) {
      testCall.cancel();
    }

    // sleep for a bit to make sure all async operations are complete, and then verify we set this value
    // in onFailure()
    Thread.sleep(500);
    assertTrue(callWasCanceled[0]);
  }
}
