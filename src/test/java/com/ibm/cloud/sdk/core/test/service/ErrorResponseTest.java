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
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceUnavailableException;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.exception.UnauthorizedException;
import com.ibm.cloud.sdk.core.service.exception.UnsupportedException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
  @Before
  public void setUp() throws Exception {
    super.setUp();
    service = new TestService(new NoAuthAuthenticator());
    service.setServiceUrl(getMockWebServerUrl());
  }

  /**
   * Test HTTP status code 400 (Bad Request) error response.
   */
  @Test
  public void testBadRequest() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(400)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof BadRequestException);
      BadRequestException ex = (BadRequestException) e;
      assertEquals(400, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 401 (Unauthorized) error response.
   */
  @Test
  public void testUnauthorized() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(401)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof UnauthorizedException);
      UnauthorizedException ex = (UnauthorizedException) e;
      assertEquals(401, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 403 (Forbidden) error response.
   */
  @Test
  public void testForbidden() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(403)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ForbiddenException);
      ForbiddenException ex = (ForbiddenException) e;
      assertEquals(403, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 404 (NotFound) error response.
   */
  @Test
  public void testNotFound() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(404)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof NotFoundException);
      NotFoundException ex = (NotFoundException) e;
      assertEquals(404, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 409 (Conflict) error response.
   */
  @Test
  public void testConflict() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(409)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ConflictException);
      ConflictException ex = (ConflictException) e;
      assertEquals(409, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 413 (RequestTooLarge) error response.
   */
  @Test
  public void testRequestTooLarge() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(413)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof RequestTooLargeException);
      RequestTooLargeException ex = (RequestTooLargeException) e;
      assertEquals(413, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 415 (Unsupported Media Type) error response.
   */
  @Test
  public void testUnsupported() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(415)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof UnsupportedException);
      UnsupportedException ex = (UnsupportedException) e;
      assertEquals(415, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 429 (TooManyRequests) error response.
   */
  @Test
  public void testTooManyRequests() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(429)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof TooManyRequestsException);
      TooManyRequestsException ex = (TooManyRequestsException) e;
      assertEquals(429, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 500 (InternalServerError) error response.
   */
  @Test
  public void testInternalServerError() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof InternalServerErrorException);
      InternalServerErrorException ex = (InternalServerErrorException) e;
      assertEquals(500, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
    }
  }

  /**
   * Test HTTP status code 503 (ServiceUnavailable) error response.
   */
  @Test
  public void testServiceUnavailable() {

    String message = "The request failed because the moon is full.";
    server.enqueue(new MockResponse()
        .setResponseCode(503)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof ServiceUnavailableException);
      ServiceUnavailableException ex = (ServiceUnavailableException) e;
      assertEquals(503, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
      assertTrue(ex.getHeaders().names().contains(CONTENT_TYPE));
      assertTrue(ex.getHeaders().values(CONTENT_TYPE).contains(HttpMediaType.APPLICATION_JSON));
    }
  }

  @Test
  public void testDebuggingInfo() {
    String message = "The request failed because the moon is full.";
    String level = "ERROR";
    String correlationId = "123456789-abcdefghi";
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"error\": \"" + message + "\"," +
            "\"level\": \"" + level + "\"," +
            "\"correlation_id\": \"" + correlationId + "\"}"));

    try {
      service.testMethod().execute();
    } catch (Exception e) {
      assertTrue(e instanceof InternalServerErrorException);
      InternalServerErrorException ex = (InternalServerErrorException) e;
      assertEquals(500, ex.getStatusCode());
      assertEquals(message, ex.getMessage());
      assertEquals(level, ex.getDebuggingInfo().get("level"));
      assertEquals(correlationId, ex.getDebuggingInfo().get("correlation_id"));
    }
  }
}
