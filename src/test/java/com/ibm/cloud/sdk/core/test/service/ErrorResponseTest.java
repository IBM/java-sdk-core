/**
 * (C) Copyright IBM Corp. 2015, 2024.
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

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.ConflictException;
import com.ibm.cloud.sdk.core.service.exception.ForbiddenException;
import com.ibm.cloud.sdk.core.service.exception.InternalServerErrorException;
import com.ibm.cloud.sdk.core.service.exception.NotAcceptableException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.service.exception.ServiceUnavailableException;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.exception.UnauthorizedException;
import com.ibm.cloud.sdk.core.service.exception.UnsupportedException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ErrorResponseTest extends BaseServiceUnitTest {

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    TestService(Authenticator auth) {
      super(SERVICE_NAME, auth);
    }

    ServiceCall<GenericModel> testMethod() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(GenericModel.class));
    }
  }

  private TestService service;

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.cloud.sdk.core.test.WatsonServiceTest#setUp()
   */
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    service = new TestService(new NoAuthAuthenticator());
    service.setServiceUrl(getMockWebServerUrl());
  }

  private void verifyException(ServiceResponseException e, int expectedStatusCode, String expectedMessage,
      String expectedResponseBody) {

    assertEquals(e.getStatusCode(), expectedStatusCode);
    assertEquals(e.getMessage(), expectedMessage);
    assertEquals(e.getResponseBody(), expectedResponseBody);
  }

  /**
   * Test HTTP status code 400 (Bad Request) error response.
   */
  @Test
  public void testBadRequest() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(400)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof BadRequestException);
      BadRequestException ex = (BadRequestException) e;
      verifyException(ex, 400, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 400 (Bad Request) error response with no response body.
   */
  @Test
  public void testBadRequestNoBody() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(400)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof BadRequestException);
      BadRequestException ex = (BadRequestException) e;
      verifyException(ex, 400, "Bad request", null);
    }
  }

  /**
   * Test HTTP status code 400 (Bad Request) error response with non-JSON response body.
   */
  @Test
  public void testBadRequestTextBody() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(400)
        .addHeader(CONTENT_TYPE, HttpMediaType.TEXT_PLAIN)
        .setBody(message));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof BadRequestException);
      BadRequestException ex = (BadRequestException) e;
      verifyException(ex, 400, message, message);
    }
  }

  /**
   * Test HTTP status code 401 (Unauthorized) error response.
   */
  @Test
  public void testUnauthorized() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"message\": \"" + message + "\", \"other_stuff\": \"foo\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(401)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof UnauthorizedException);
      UnauthorizedException ex = (UnauthorizedException) e;
      verifyException(ex, 401, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 403 (Forbidden) error response.
   */
  @Test
  public void testForbidden() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"errors\": [{\"message\": \"" + message + "\"}, {\"message\": \"another error...\"}]}";
    server.enqueue(new MockResponse()
        .setResponseCode(403)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ForbiddenException);
      ForbiddenException ex = (ForbiddenException) e;
      verifyException(ex, 403, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 406 (NotAcceptable) error response.
   */
  @Test
  public void testNotAcceptable() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"errorMessage\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(406)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof NotAcceptableException);
      NotAcceptableException ex = (NotAcceptableException) e;
      verifyException(ex, 406, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 404 (NotFound) error response.
   */
  @Test
  public void testNotFound() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(404)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof NotFoundException);
      NotFoundException ex = (NotFoundException) e;
      verifyException(ex, 404, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 409 (Conflict) error response.
   */
  @Test
  public void testConflict() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(409)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ConflictException);
      ConflictException ex = (ConflictException) e;
      verifyException(ex, 409, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 413 (RequestTooLarge) error response.
   */
  @Test
  public void testRequestTooLarge() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(413)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof RequestTooLargeException);
      RequestTooLargeException ex = (RequestTooLargeException) e;
      verifyException(ex, 413, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 415 (Unsupported Media Type) error response.
   */
  @Test
  public void testUnsupported() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(415)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof UnsupportedException);
      UnsupportedException ex = (UnsupportedException) e;
      verifyException(ex, 415, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 429 (TooManyRequests) error response.
   */
  @Test
  public void testTooManyRequests() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(429)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof TooManyRequestsException);
      TooManyRequestsException ex = (TooManyRequestsException) e;
      verifyException(ex, 429, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 500 (InternalServerError) error response.
   */
  @Test
  public void testInternalServerError() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof InternalServerErrorException);
      InternalServerErrorException ex = (InternalServerErrorException) e;
      verifyException(ex, 500, message, responseBody);
    }
  }

  /**
   * Test HTTP status code 503 (ServiceUnavailable) error response.
   */
  @Test
  public void testServiceUnavailable() {

    String message = "The request failed because the moon is full.";
    String responseBody = "{\"error\": \"" + message + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(503)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ServiceUnavailableException);
      ServiceUnavailableException ex = (ServiceUnavailableException) e;
      verifyException(ex, 503, message, responseBody);
      assertTrue(ex.getHeaders().names().contains(CONTENT_TYPE));
      assertTrue(ex.getHeaders().values(CONTENT_TYPE).contains(HttpMediaType.APPLICATION_JSON));
    }
  }

  @Test
  public void testDebuggingInfo() {
    String message = "The request failed because the moon is full.";
    String level = "ERROR";
    String correlationId = "123456789-abcdefghi";
    String responseBody = "{\"error\": \"" + message + "\"," + "\"level\": \"" + level + "\","
        + "\"correlation_id\": \"" + correlationId + "\"}";
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody(responseBody));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof InternalServerErrorException);
      InternalServerErrorException ex = (InternalServerErrorException) e;
      verifyException(ex, 500, message, responseBody);
      assertEquals(ex.getDebuggingInfo().get("level"), level);
      assertEquals(ex.getDebuggingInfo().get("correlation_id"), correlationId);
    }
  }
}
