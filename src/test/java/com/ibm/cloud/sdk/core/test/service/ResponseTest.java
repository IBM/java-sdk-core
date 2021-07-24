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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.exception.InvalidServiceResponseException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.Clock;
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

    ServiceCall<TestModel> getTestModelPOJO() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }

    ServiceCall<TestModel> getTestModelGenericType() {
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

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    service = new TestService(new NoAuthAuthenticator());
    service.setServiceUrl(getMockWebServerUrl());
  }

  @Test
  public void testAllFieldsShouldBePopulatedWhenTheResponseIsPOJO() {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelPOJO().execute();

    // Assert
    assertNotNull(response.getResult());
    assertEquals(expectedValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testNullResultShouldReturnWhenNoResponse() {
    // Arrange
    String responseBody = "";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelPOJO().execute();

    // Assert
    assertNull(response.getResult());
  }

  @Test
  public void testShouldReturnEmptyModelWhenResponseIsAnEmptyObject() {
    // Arrange
    String responseBody = "{}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelPOJO().execute();

    // Assert
    TestModel obj = response.getResult();
    assertNotNull(obj);
    assertNull(obj.getCity());
  }

  @Test(expectedExceptions = InvalidServiceResponseException.class)
  public void testShouldThrowWhenJsonResponseIsInvalidJson() {
    // Arrange
    String responseBody = "{\"city\": \"Colum";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    service.getTestModelPOJO().execute();
  }

  @Test
  public void testShouldPopulateAllFieldsWhenCallingEnqueue() throws InterruptedException {
    // Arrange
    String expectedValueFromResultModel = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValueFromResultModel);
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    service.getTestModelPOJO().enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        results.put("method", "onResponse");
        results.put("response", response);
      }

      @Override
      public void onFailure(Exception e) {
        results.put("method", "onFailure");
      }
    });

    // Assert
    Thread.sleep(2000);

    assertEquals("onResponse", results.get("method"));
    Response<TestModel> response = (Response<TestModel>) results.get("response");
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertEquals(expectedValueFromResultModel, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testShouldThrowWhenInvalidJsonIsEnqueued() throws InterruptedException{
    // Arrange
    String responseBody = "{\"city\": \"Colum";
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    service.getTestModelPOJO().enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        results.put("method", "onResponse");
      }

      @Override
      public void onFailure(Exception e) {
        results.put("method", "onFailure");
        results.put("exception", e);
      }
    });

    // Assert
    Thread.sleep(2000);

    assertEquals("onFailure", results.get("method"));
    Throwable t = (Throwable) results.get("exception");
    assertNotNull(t);
    assertTrue(t instanceof InvalidServiceResponseException);
  }


  @Test
  public void testReactiveRequestShouldCompleteCorrectly() throws InterruptedException {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    Single<Response<TestModel>> observableRequest = service.getTestModelPOJO().reactiveRequest();

    // Assert
    observableRequest
        .subscribeOn(Schedulers.single())
        .subscribe(new Consumer<Response<TestModel>>() {
          @Override
          public void accept(Response<TestModel> response) throws Exception {
            results.put("response", response);
          }
        });

    // asynchronous, so test that we continued without a value yet
    assertNull(results.get("response"));

    Thread.sleep(2000);

    Response<TestModel> response = (Response<TestModel>) results.get("response");
    assertNotNull(response);
    assertEquals(expectedValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testHeadersShouldBeAccessible() {
    // Arrange
    Headers rawHeaders = Headers.of("Content-Length", "472", "Content-Type", "application/json", "Server", "Mock");
    com.ibm.cloud.sdk.core.http.Headers expectedHeaders = new com.ibm.cloud.sdk.core.http.Headers(rawHeaders);
    server.enqueue(new MockResponse().setHeaders(rawHeaders));

    // Act
    Response<Void> response = service.headMethod().execute();

    // Assert
    com.ibm.cloud.sdk.core.http.Headers actualHeaders = response.getHeaders();
    assertNull(response.getResult());
    assertNotNull(actualHeaders);

    // We can't just compare expectedHeaders and actualHeaders) because of some
    // underlying weirdness in okhttp's Headers class, so we'll compare the
    // toString() output of each one instead.
    // System.out.println("Expected headers:\n" + expectedHeaders.toString());
    // System.out.println("Actual headers:\n" + actualHeaders.toString());
    assertEquals(expectedHeaders.toString(), actualHeaders.toString());
  }

  @Test
  public void testAllFieldsShouldBePopulatedWhenResultIsGenericType() {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelGenericType().execute();

    // Assert
    assertNotNull(response.getResult());
    assertEquals(expectedValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testShouldReturnStringDeserializedCorrectly() {
    // Arrange
    String expectedResult = "string response";
    String responseBody = String.format("\"%s\"", expectedResult);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getString().execute();

    // Assert
    String result = response.getResult();
    assertNotNull(result);
    assertEquals(expectedResult, result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testShouldReturnListOfStringsDeserializedCorrectly() {
    // Arrange
    String responseBody = "[\"string1\",\"string2\",\"string3\"]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<String>> response = service.getListString().execute();

    // Arrange
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", "string2", "string3"), result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testShouldReturnLongDeserializedCorrectly() {
    // Arrange
    String responseBody = "443374";
    Long expectedResult = Long.parseLong(responseBody);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<Long> response = service.getLong().execute();

    // Assert
    Long result = response.getResult();
    assertNotNull(result);
    assertEquals(expectedResult, result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testShouldReturnListOfLongsDeserializedCorrectly() {
    // Arrange
    String responseBody = "[44,33,74]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<Long>> response = service.getListLong().execute();

    // Assert
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
   */
  @Test
  public void testShouldReturnListOfObjectDeserializedCorrectly() {
    // Arrange
    String responseBody = "[{\"city\":\"Austin\"},{\"city\":\"Georgetown\"},{\"city\":\"Cedar Park\"}]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListTestModel().execute();

    // Assert
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

  @Test
  public void testShouldReturnNullListWhenResponseBodyIsMissing() {
    // Arrange
    String responseBody = "";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListTestModel().execute();

    // Assert
    List<TestModel> list = response.getResult();
    assertNull(list);
  }

  @Test
  public void testShouldReturnEmptyListWhenResponseBodyIsAnEmptyList() {
    // Arrange
    String responseBody = "[]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListTestModel().execute();

    // Assert
    List<TestModel> list = response.getResult();
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  @Test
  public void testShouldReturnTheExpectedResponseCode() {
    // Arrange
    server.enqueue(new MockResponse().setResponseCode(204));

    // Act
    Response<Void> response = service.headMethod().execute();

    // Assert
    assertEquals(204, response.getStatusCode(), "The response status code should be 204.");
  }

  @Test
  public void testShouldBeAbleToRetrieveStatusLineFromResponseMessage() {
    // Arrange
    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 No Content"));

    // Act
    Response<Void> response = service.headMethod().execute();

    // Assert
    assertEquals("No Content", response.getStatusMessage(), "The response status message should be 'No Content'.");
  }

  @Test
  public void testShouldBeAbleToCancelRequestCall() throws InterruptedException {
    // Arrange
    String responseBody = "{\"city\": \"Columbus\"}";
    server.enqueue(new MockResponse().setBody(responseBody).setBodyDelay(5000, TimeUnit.MILLISECONDS));

    // time to consider timeout (in ms)
    long timeoutThreshold = 3000;
    final boolean[] hasCallCompleted = {false};
    final boolean[] callWasCanceled = {false};

    ServiceCall<TestModel> testCall = service.getTestModelPOJO();
    long startTime = Clock.getCurrentTimeInMillis();
    testCall.enqueue(new ServiceCallback<TestModel>() {
      @Override
      public void onResponse(Response<TestModel> response) {
        hasCallCompleted[0] = true;
        // System.out.println("We got a response!");
      }

      @Override
      public void onFailure(Exception e) {
        callWasCanceled[0] = true;
        // System.out.println("The request failed as expected :)");
      }
    });

    // keep waiting for the call to complete while we're within the timeout bounds
    while (!hasCallCompleted[0] && (Clock.getCurrentTimeInMillis() - startTime < timeoutThreshold)) {
      Thread.sleep(500);
    }

    // if we timed out and it's STILL not complete, we'll just cancel the call
    // Act
    if (!hasCallCompleted[0]) {
      testCall.cancel();
    }

    // Assert
    // sleep for a bit to make sure all async operations are complete, and then verify we set this value
    // in onFailure()
    Thread.sleep(500);
    assertTrue(callWasCanceled[0]);
  }
}
