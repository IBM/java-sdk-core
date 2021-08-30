/**
 * (C) Copyright IBM Corp. 2021.
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

package com.ibm.cloud.sdk.core.test.http.retry;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_ENCODING;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RetryTest extends BaseServiceUnitTest {

    public class TestModel extends GenericModel {
        String success;

        public String getSuccess() {
            return success;
        }
    }

    public class TestService extends BaseService {

        private static final String SERVICE_NAME = "test";

        TestService(Authenticator auth) {
            super(SERVICE_NAME, auth);
        }

        ServiceCall<TestModel> testMethod() {
            RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
            return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
        }
    }

    private RetryTest.TestService service;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        service = new RetryTest.TestService(new NoAuthAuthenticator());
        service.enableRetries(3, 10);
        service.setServiceUrl(getMockWebServerUrl());
    }

    /**
     * Test that we don't endlessly retry on 429.
     */
    @Test
    public void testRetryIsExhausted() {

        String message = "The request failed because the moon is full.";
        for (int i = 0; i <= 5; i++) {
            server.enqueue(
                    new MockResponse()
                            .setResponseCode(429)
                            .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                            .setBody("{\"error\": \"" + message + "\"}"));
        }
        try {
            service.testMethod().execute();
        } catch (Exception e) {
            assertTrue(e instanceof TooManyRequestsException);
            TooManyRequestsException ex = (TooManyRequestsException) e;
            assertEquals(429, ex.getStatusCode());
            assertEquals(message, ex.getMessage());
            assertEquals(4, server.getRequestCount());
        }
    }

    /**
     * Test that we retry on 429.
     */
    @Test
    public void testRetrySuccess() throws Throwable {

        String message = "The request failed because the moon is full.";
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"error\": \"" + message + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));

        Response<TestModel> r = service.testMethod().execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(2, server.getRequestCount());

        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertNull(request.getHeader(CONTENT_ENCODING));
    }

    /**
     * Test that we don't retry on 501.
     */
    @Test
    public void testDoNotRetry501() {

        String message = "What is this?";
        server.enqueue(new MockResponse()
                .setResponseCode(501)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"error\": \"" + message + "\"}"));

        try {
            service.testMethod().execute();
        } catch (Exception e) {
            assertTrue(e instanceof ServiceResponseException);
            ServiceResponseException ex  = (ServiceResponseException) e;
            assertEquals(501, ex.getStatusCode());
            assertEquals(ex.getMessage(), message);
            assertEquals(1, server.getRequestCount());
        }
    }

     /**
     * Test that we take care of valid Retry-After headers.
     */
    @Test(timeOut = 6000)
    public void testValidRetryAfter() {

        String message = "The request failed because the moon is full.";
        server.enqueue(new MockResponse()
                .setResponseCode(504)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .addHeader("Retry-After", "5")
                .setBody("{\"error\": \"" + message + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));


        Response<TestModel> r = service.testMethod().execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(2, server.getRequestCount());

    }

    /**
     * Test that we take care of invalid Retry-After headers.
     */
    @Test(timeOut = 2000)
    public void testInvalidRetryAfter() {

        server.enqueue(new MockResponse()
                .setResponseCode(504)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .addHeader("Retry-After", "-1")
                .setBody("{\"error\": \"this is not valid\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));


        Response<TestModel> r = service.testMethod().execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(2, server.getRequestCount());
    }

    /**
     * Test the HTTP Time format from the `Retry-After` header in the response.
     */
    @Test(timeOut = 4000)
    public void testHttpTimeRetry() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).withZone(ZoneOffset.UTC);
        Instant now = Instant.now().plusSeconds(3);
        String after = f.format(now);

        server.enqueue(new MockResponse()
                .setResponseCode(504)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .addHeader("Retry-After", after)
                .setBody("{\"error\": \"Be patient!\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));


        Response<TestModel> r = service.testMethod().execute();

        assertEquals(200, r.getStatusCode());
        assertEquals(r.getResult().getSuccess(), "awesome");
        assertEquals(2, server.getRequestCount());
    }

    @Test(timeOut = 5000)
    public void testMaxRetryInterval() {
        service.enableRetries(10, 1);

        String message = "phew";
        for (int i = 0; i < 4; i++) {
            server.enqueue(
                    new MockResponse()
                            .setResponseCode(429)
                            .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                            .setBody("{\"error\": \"too fast\"}"));
        }
        server.enqueue(
            new MockResponse()
                    .setResponseCode(200)
                    .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                    .setBody("{\"success\": \"" + message + "\"}"));

        Response<TestModel> r = service.testMethod().execute();

        assertEquals(200, r.getStatusCode());
        assertEquals(message, r.getResult().getSuccess());
        assertEquals(5, server.getRequestCount());
    }

    @Test
    public void testDisableRetries() {
        service.disableRetries();

        String message = "retry please";
        for (int i = 0; i <= 5; i++) {
            server.enqueue(
                    new MockResponse()
                            .setResponseCode(429)
                            .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                            .setBody("{\"error\": \"" + message + "\"}"));
        }
        try {
            service.testMethod().execute();
        } catch (Exception e) {
            assertTrue(e instanceof TooManyRequestsException);
            TooManyRequestsException ex = (TooManyRequestsException) e;
            assertEquals(429, ex.getStatusCode());
            assertEquals(message, ex.getMessage());
            assertEquals(1, server.getRequestCount());
        }
    }
}
