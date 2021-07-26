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
import com.google.gson.annotations.SerializedName;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ResponseTest extends BaseServiceUnitTest {
  private class TestModel extends GenericModel {
    String city;

    String getCity() {
      return city;
    }
  }

  public class GenericObjectModel extends GenericModel {
    @SerializedName("name")
    String name;

    @SerializedName("address")
    String address;

    @SerializedName("age")
    Integer age;

    public String getName() {
      return name;
    }

    public String getAddress() {
      return address;
    }

    public Integer getAge() {
      return age;
    }
  }

  public class PassportModel {
    @SerializedName("serial")
    private String serial;

    @SerializedName("issuer")
    private String issuer;

    public String getSerial() {
      return serial;
    }

    public String getIssuer() {
      return issuer;
    }
  }

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    TestService(Authenticator auth) {
      super(SERVICE_NAME, auth);
    }

    ServiceCall<TestModel> getTestModelByResponseConverterUtilsGetObject() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }

    ServiceCall<TestModel> getTestModelByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<TestModel> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<TestModel>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<Void> headMethod() {
      RequestBuilder builder = RequestBuilder.head(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getVoid());
    }

    ServiceCall<String> getStringByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<String> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<String>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<String>> getListOfStringsByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<String>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<String>>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<Long> getLongByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<Long> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<Long>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<Long>> getListLongValuesByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<Long>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<Long>>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<List<TestModel>> getListOfTestModelsByResponseConverterUtilsGetValue() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      ResponseConverter<List<TestModel>> responseConverter =
          ResponseConverterUtils.getValue(new TypeToken<List<TestModel>>() {
          }.getType());
      return createServiceCall(builder.build(), responseConverter);
    }

    ServiceCall<String> getStringByResponseConverterUtilsGenericObject(final String propertyName) {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(),
          ResponseConverterUtils.getGenericObject(String.class, propertyName));
    }

    ServiceCall<PassportModel> getPassportModelByResponseConverterUtilsGenericObject(
        final String propertyName) {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(),
          ResponseConverterUtils.getGenericObject(PassportModel.class, propertyName));
    }

    ServiceCall<String> getStringRepresentationOfResponseBodyByResponseConverterUtilsGetString() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.get(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getString());
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
  public void testGetObjectShouldReturnAnObjectAllFieldsPopulatedWhenResponseBodyIsAPOJO() {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetObject().execute();

    // Assert
    assertNotNull(response.getResult());
    assertEquals(expectedValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetObjectShouldReturnNullWhenResponseBodyIsEmpty() {
    // Arrange
    String responseBody = "";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetObject().execute();

    // Assert
    assertNull(response.getResult());
  }

  @Test
  public void testGetObjectShouldReturnEmptyModelWhenResponseBodyIsEmptyObject() {
    // Arrange
    String responseBody = "{}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetObject().execute();

    // Assert
    TestModel obj = response.getResult();
    assertNotNull(obj);
    assertNull(obj.getCity());
  }

  @Test(expectedExceptions = InvalidServiceResponseException.class)
  public void testGetObjectShouldThrowWhenResponseBodyIsInvalidJson() {
    // Arrange
    String responseBody = "{\"city\": \"Colum";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    service.getTestModelByResponseConverterUtilsGetObject().execute();
  }

  @Test
  public void testGetObjectShouldPopulateAllFieldsWhenCallingEnqueue() throws InterruptedException {
    // Arrange
    String expectedValueFromResultModel = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValueFromResultModel);
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    service.getTestModelByResponseConverterUtilsGetObject().enqueue(new ServiceCallback<TestModel>() {
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
  public void testGetObjectShouldThrowWhenInvalidJsonIsEnqueued() throws InterruptedException {
    // Arrange
    String responseBody = "{\"city\": \"Colum";
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    service.getTestModelByResponseConverterUtilsGetObject().enqueue(new ServiceCallback<TestModel>() {
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
  public void testGetObjectReactiveRequestShouldCompleteCorrectly() throws InterruptedException {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    final Map<String, Object> results = new HashMap<>();

    // Act
    Single<Response<TestModel>> observableRequest = service.getTestModelByResponseConverterUtilsGetObject()
        .reactiveRequest();

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
  public void testGetValueShouldReturnObjectWithAllFieldsPopulatedWhenResponseBodyIsPOJO() {
    // Arrange
    String expectedValue = "Columbus";
    String responseBody = String.format("{\"city\": \"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetValue().execute();

    // Assert
    assertNotNull(response.getResult());
    assertEquals(expectedValue, response.getResult().getCity());
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnNullWhenResponseBodyIsEmpty() {
    // Arrange
    String responseBody = "";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetValue().execute();

    // Assert
    assertNotNull(response);
    assertNull(response.getResult());
  }

  @Test
  public void testGetValueShouldReturnEmptyModelWhenResponseBodyIsEmpty() {
    // Arrange
    String responseBody = "{}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<TestModel> response = service.getTestModelByResponseConverterUtilsGetValue().execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertNull(response.getResult().getCity());
  }

  @Test(expectedExceptions = InvalidServiceResponseException.class)
  public void testGetValueShouldThrowWhenResponseBodyIsInvalidJson() {
    // Arrange
    String responseBody = "{\"city\": \"Colum";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    service.getTestModelByResponseConverterUtilsGetValue().execute();
  }

  @Test
  public void testGetValueShouldReturnStringDeserializedCorrectly() {
    // Arrange
    String expectedResult = "string response";
    String responseBody = String.format("\"%s\"", expectedResult);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringByResponseConverterUtilsGetValue().execute();

    // Assert
    String result = response.getResult();
    assertNotNull(result);
    assertEquals(expectedResult, result);
    assertNotNull(response.getHeaders());
  }

  @DataProvider(name = "testGetValueShouldReturnString")
  public static Object[][] testGetValueShouldReturnStringDataProvider() {
    return new Object[][]{
        {""},
        {" "},
        };
  }

  @Test(dataProvider = "testGetValueShouldReturnString")
  public void testGetValueShouldReturnNullWhenStringResponseIsEmptyOrWhitespace(String responseBody) {
    // Arrange
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringByResponseConverterUtilsGetValue().execute();

    // Assert
    String result = response.getResult();
    assertNull(result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnListOfStringsDeserializedCorrectly() {
    // Arrange
    String responseBody = "[\"string1\",\"string2\",\"string3\"]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<String>> response = service.getListOfStringsByResponseConverterUtilsGetValue().execute();

    // Arrange
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", "string2", "string3"), result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnListOfStringsDeserializedCorrectlyWhenAnItemIsNullValue() {
    // Arrange
    String responseBody = "[\"string1\",null ,\"string3\"]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<String>> response = service.getListOfStringsByResponseConverterUtilsGetValue().execute();

    // Arrange
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", null, "string3"), result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnListOfStringsDeserializedCorrectlyWhenAnItemIsEmptyString() {
    // Arrange
    String responseBody = "[\"string1\",\"\" ,\"string3\"]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<String>> response = service.getListOfStringsByResponseConverterUtilsGetValue().execute();

    // Arrange
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", "", "string3"), result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnListOfStringsDeserializedCorrectlyWhenAnItemIsWhitespace() {
    // Arrange
    String responseBody = "[\"string1\",\" \" ,\"string3\"]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<String>> response = service.getListOfStringsByResponseConverterUtilsGetValue().execute();

    // Arrange
    List<String> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    assertEquals(Arrays.asList("string1", " ", "string3"), result);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnLongDeserializedCorrectly() {
    // Arrange
    String responseBody = "443374";
    Long expectedResult = Long.parseLong(responseBody);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<Long> response = service.getLongByResponseConverterUtilsGetValue().execute();

    // Assert
    Long result = response.getResult();
    assertNotNull(result);
    assertEquals(expectedResult, result);
    assertNotNull(response.getHeaders());
  }

  @Test(expectedExceptions = {InvalidServiceResponseException.class})
  public void testGetValueShouldThrowWhenLongValueIsInvalid() {
    // Arrange
    String responseBody = "443f374";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    service.getLongByResponseConverterUtilsGetValue().execute();
  }

  @Test
  public void testGetValueShouldReturnListOfLongsDeserializedCorrectly() {
    // Arrange
    String responseBody = "[44,33,74]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<Long>> response = service.getListLongValuesByResponseConverterUtilsGetValue().execute();

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

  @Test(expectedExceptions = {InvalidServiceResponseException.class})
  public void testGetValueShouldThrowWhenInvalidLongIsInTheList() {
    // Arrange
    String responseBody = "[44,3f3,74]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    service.getListLongValuesByResponseConverterUtilsGetValue().execute();
  }

  @Test
  public void testGetValueShouldReturnListOfObjectsDeserializedCorrectly() {
    // Arrange
    String responseBody = "[{\"city\":\"Austin\"},{\"city\":\"Georgetown\"},{\"city\":\"Cedar Park\"}]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListOfTestModelsByResponseConverterUtilsGetValue().execute();

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
  public void testGetValueShouldReturnListOfObjectsDeserializedCorrectlyWhenOneFromTheItemIsNull() {
    // Arrange
    String responseBody = "[{\"city\":\"Austin\"},null ,{\"city\":\"Cedar Park\"}]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListOfTestModelsByResponseConverterUtilsGetValue().execute();

    // Assert
    List<TestModel> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    List<String> actualCities = new ArrayList<>();
    for (TestModel obj : result) {
      if (obj != null && obj.getCity() != null) {
        actualCities.add(obj.getCity());
      }
    }
    assertEquals(Arrays.asList("Austin", "Cedar Park"), actualCities);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnListOfObjectsDeserializedCorrectlyWhenFromTheItemsIsEmptyObject() {
    // Arrange
    String responseBody = "[{\"city\":\"Austin\"},{} ,{\"city\":\"Cedar Park\"}]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListOfTestModelsByResponseConverterUtilsGetValue().execute();

    // Assert
    List<TestModel> result = response.getResult();
    assertNotNull(result);
    assertEquals(result.size(), 3);
    List<String> actualCities = new ArrayList<>();
    for (TestModel obj : result) {
      if (obj != null && obj.getCity() != null) {
        actualCities.add(obj.getCity());
      }
    }
    assertEquals(Arrays.asList("Austin", "Cedar Park"), actualCities);
    assertNotNull(response.getHeaders());
  }

  @Test
  public void testGetValueShouldReturnNullListWhenResponseBodyIsMissing() {
    // Arrange
    String responseBody = "";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListOfTestModelsByResponseConverterUtilsGetValue().execute();

    // Assert
    List<TestModel> list = response.getResult();
    assertNull(list);
  }

  @Test
  public void testGetValueShouldReturnEmptyListWhenResponseBodyIsAnEmptyList() {
    // Arrange
    String responseBody = "[]";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<List<TestModel>> response = service.getListOfTestModelsByResponseConverterUtilsGetValue().execute();

    // Assert
    List<TestModel> list = response.getResult();
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  @Test
  public void testHeadMethodShouldReturnTheExpectedResponseCode() {
    // Arrange
    server.enqueue(new MockResponse().setResponseCode(204));

    // Act
    Response<Void> response = service.headMethod().execute();

    // Assert
    assertEquals(204, response.getStatusCode(), "The response status code should be 204.");
  }

  @Test
  public void testHeadMethodShouldBeAbleToRetrieveStatusLineFromResponseMessage() {
    // Arrange
    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 No Content"));

    // Act
    Response<Void> response = service.headMethod().execute();

    // Assert
    assertEquals("No Content", response.getStatusMessage(), "The response status message should be 'No Content'.");
  }

  @Test
  public void testGetObjectShouldBeAbleToCancelRequestCall() throws InterruptedException {
    // Arrange
    String responseBody = "{\"city\": \"Columbus\"}";
    server.enqueue(new MockResponse().setBody(responseBody).setBodyDelay(5000, TimeUnit.MILLISECONDS));

    // time to consider timeout (in ms)
    long timeoutThreshold = 3000;
    final boolean[] hasCallCompleted = {false};
    final boolean[] callWasCanceled = {false};

    ServiceCall<TestModel> testCall = service.getTestModelByResponseConverterUtilsGetObject();
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

  @DataProvider(name = "stringResponseBodyMemberIsNullOrEmptyOrWhitespace")
  public static Object[][] stringResponseBodyMemberIsNullOrEmptyOrWhitespaceDataProvider() {
    return new Object[][]{
        {"null"},
        {""},
        {" "}
    };
  }

  @Test(dataProvider = "stringResponseBodyMemberIsNullOrEmptyOrWhitespace")
  public void testGenericObjectStringShouldReturnNullValueWhenResponseBodyStringValueIsEmptyOrNullValue(
      String expectedValue
  ) {
    // Arrange
    String responseBody = String.format("{\"name\":\"%s\"}", expectedValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringByResponseConverterUtilsGenericObject("name").execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().equals(expectedValue));
  }

  @Test
  public void testGenericObjectStringShouldReturnStringValue() {
    // Arrange
    String expectedNameValue = "lorem";
    String responseBody = String.format("{\"name\":\"%s\"}", expectedNameValue);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringByResponseConverterUtilsGenericObject("name").execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertEquals(response.getResult(), expectedNameValue);
  }

  @Test
  public void testGenericObjectShouldReturnNullObjectWhenResponseBodyMemberIsNullValue() {
    // Arrange
    String responseBody = "{\"name\"=\"Lorem\", \"age\":\"45\", \"passport\":null}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<PassportModel> response = service.getPassportModelByResponseConverterUtilsGenericObject("passport")
        .execute();

    // Assert
    assertNotNull(response);
    assertNull(response.getResult());
  }

  @Test
  public void testGenericObjectShouldReturnEmptyDefinedTypeWhenResponseBodyMemberIsEmptyObject() {
    // Arrange
    String responseBody = "{\"name\"=\"Lorem\", \"age\":\"45\", \"passport\":{}}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<PassportModel> response = service.getPassportModelByResponseConverterUtilsGenericObject("passport")
        .execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertNull(response.getResult().getIssuer());
    assertNull(response.getResult().getSerial());
  }

  @Test
  public void testGenericObjectShouldReturnDefinedType() {
    // Arrange
    String expectedPassportIssuer = "Office";
    String expectedPassportSerialNumber = "AB12345";
    String passportModel = String.format("{\"issuer\":\"%s\", \"serial\":\"%s\"}",
        expectedPassportIssuer,
        expectedPassportSerialNumber);
    String responseBody = String.format("{\"name\"=\"Lorem\", \"age\":\"45\", \"passport\":%s}", passportModel);
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<PassportModel> response = service.getPassportModelByResponseConverterUtilsGenericObject("passport")
        .execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertEquals(response.getResult().getSerial(), expectedPassportSerialNumber);
    assertEquals(response.getResult().getIssuer(), expectedPassportIssuer);
  }

  @Test
  public void testGetResponseBodyAsStringShouldReturnStringRepresentationOfResponseBody() {
    // Arrange
    String responseBody = "{\"name\":\"Lorem\", \"age\":\"45\"}";
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringRepresentationOfResponseBodyByResponseConverterUtilsGetString()
        .execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertEquals(response.getResult(), responseBody);
  }

  @DataProvider(name = "getResponseBodyStringRepresentation")
  public static Object[][] getResponseBodyStringRepresentationDataProvider() {
    return new Object[][]{
        {""},
        {" "}
    };
  }

  @Test(dataProvider = "getResponseBodyStringRepresentation")
  public void testGetResponseBodyAsStringShouldReturnNullWhenResponseBodyIsEmptyOrWhitespace(String responseBody) {
    // Arrange
    server.enqueue(new MockResponse().setBody(responseBody));

    // Act
    Response<String> response = service.getStringRepresentationOfResponseBodyByResponseConverterUtilsGetString()
        .execute();

    // Assert
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertEquals(response.getResult(), responseBody);
  }

}
