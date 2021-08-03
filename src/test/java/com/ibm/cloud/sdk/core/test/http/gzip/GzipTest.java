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
package com.ibm.cloud.sdk.core.test.http.gzip;

import com.google.gson.JsonObject;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import org.testng.annotations.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.ACCEPT_ENCODING;
import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_ENCODING;
import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.GZIPInputStream;

public class GzipTest extends BaseServiceUnitTest {

	public class TestModel extends GenericModel {
        String success;

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String message) {
            this.success = message;
        }
    }


    public class TestService extends BaseService {

        private static final String SERVICE_NAME = "test";

        TestService(Authenticator auth) {
            super(SERVICE_NAME, auth);
        }

        ServiceCall<TestModel> testMethod(RequestBuilder builder) {
            return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
        }

        ServiceCall<Void> testMethodVoid(RequestBuilder builder) {
            return createServiceCall(builder.build(), ResponseConverterUtils.getVoid());
        }
    }

    private GzipTest.TestService service;

    public void setUp(boolean enableGzip, boolean enableRateLimit) throws Exception {
        super.setUp();
        service = new GzipTest.TestService(new NoAuthAuthenticator());

        HttpConfigOptions.Builder builder = new HttpConfigOptions.Builder();
        if (enableGzip) {
            builder.enableGzipCompression(true);
        }
        if (enableRateLimit) {
            builder.enableRateLimitRetry(new NoAuthAuthenticator(),1,3);
        }
        service.configureClient(builder.build());
        service.setServiceUrl(getMockWebServerUrl());
    }

    private Buffer gzip(String data) throws IOException {
        Buffer result = new Buffer();
        BufferedSink sink = Okio.buffer(new GzipSink(result));
        sink.writeUtf8(data);
        sink.close();
        return result;
    }

    private String ungzipRequestBody(Buffer requestBody) {
        String body = null;
        String charset = "UTF-8";
        try (
            InputStream gzippedResponse = requestBody.inputStream();
            InputStream ungzippedResponse = new GZIPInputStream(gzippedResponse);
            Reader reader = new InputStreamReader(ungzippedResponse, charset);
            Writer writer = new StringWriter();
        ) {
            char[] buffer = new char[10240];
            for (int length; (length = reader.read(buffer)) > 0; ) {
                writer.write(buffer, 0, length);
            }
            body = writer.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    @Test
    public void testCompressionDisabledWithBodyJsonObjectPost() throws Throwable {
        boolean enableGzip = false;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final TestModel model = new TestModel();
        model.setSuccess("awesome");

        final JsonObject contentJson = new JsonObject();
        contentJson.addProperty("success", model.getSuccess());

        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Accept", "application/json");
        builder.bodyJson(contentJson).build();

        // queue the response to the mock server
        String mockResponseBody = contentJson.toString();
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "application/json"));

        int bodySize = contentJson.toString().length();

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(201, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        // Verify the request was not compressed, content encoding, & content length
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Request should not be compressed
        assertEquals(contentJson.toString(), request.getBody().readUtf8());
    }

    @Test
    public void testReqResponseCompressionWithBodyJsonObjectPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final TestModel model = new TestModel();
        model.setSuccess("awesome");

        final JsonObject contentJson = new JsonObject();
        contentJson.addProperty("success", model.getSuccess());

        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Accept", "application/json");
        builder.bodyJson(contentJson).build();

        // queue the response to the mock server
        Buffer mockResponseBody = gzip(contentJson.toString());
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-Encoding", "gzip")
            .setHeader("Content-type", "application/json"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(contentJson.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(201, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        // Verify the request was indeed compressed, content encoding, & content length
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(contentJson.toString(), ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyJsonObjectPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final TestModel model = new TestModel();
        model.setSuccess("awesome");

        final JsonObject contentJson = new JsonObject();
        contentJson.addProperty("success", model.getSuccess());

        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Accept", "application/json");
        builder.bodyJson(contentJson).build();

        // queue the response to the mock server
        String mockResponseBody = contentJson.toString();
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "application/json"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(contentJson.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(201, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        // Verify the request was indeed compressed, content encoding, & content length
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(mockResponseBody, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyJsonObjectPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final TestModel model = new TestModel();
        model.setSuccess("awesome");

        final JsonObject contentJson = new JsonObject();
        contentJson.addProperty("success", model.getSuccess());

        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Accept", "application/json");
        builder.bodyJson(contentJson).build();

        // queue the response to the mock server
        String mockResponseBody = contentJson.toString();
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "application/json"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(contentJson.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(200, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        // Verify the request was indeed compressed, content encoding, & content length
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(mockResponseBody, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyJsonObjectGet() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        final RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));

        // queue the response to the mock server
        String mockResponseBody = "{\"success\": \"awesome\"}";
        server.enqueue(new MockResponse()
            .setHeader("Content-type", "application/json")
            .setResponseCode(200)
            .setBody(mockResponseBody));

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(200, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "GET");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(0, request.getBodySize());
    }

    @Test
    public void testCompressionWithBodyJsonObjectHead() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        final RequestBuilder builder = RequestBuilder.head(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));

        // queue the response to the mock server
        server.enqueue(new MockResponse()
            .setHeader("Content-type", "application/json")
            .setResponseCode(200));

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());

        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "HEAD");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(0, request.getBodySize());
    }

    @Test
    public void testCompressionWithBodyOctetStreamPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String mockPayload = "This is a mock file.";
        final InputStream payload = new ByteArrayInputStream(mockPayload.getBytes());
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "application/octet-stream").build();

        // queue the response to the mock server
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-type", "application/octet-stream"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());

        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertTrue(request.getBodySize() > 0);
        // Uncompress the request body and validate
        assertEquals(mockPayload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyOctetStreamPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String mockPayload = "This is a mock file.";
        final InputStream payload = new ByteArrayInputStream(mockPayload.getBytes());
        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "application/octet-stream").build();

        // queue the response to the mock server
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-type", "application/octet-stream"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());

        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertTrue(request.getBodySize() > 0);
        // Uncompress the request body and validate
        assertEquals(mockPayload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyTextPlainPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/plain").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/plain"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyTextPlainPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/plain").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/plain"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyTextHtmlPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/html").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/html"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyTextHtmlPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/html").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/html"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testReqResponseCompressionWithBodyTextHtmlPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/html").build();

        // queue the response to the mock server
        Buffer mockResponseBody = gzip(payload);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-Encoding", "gzip")
            .setHeader("Content-type", "text/html"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        // Uncompress the request body and validate
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testCompressionWithBodyMultiPartFormPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        final String payload = "This is a mock payload.";
        multipartBuilder.addFormDataPart("string_prop", payload);
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.body(multipartBuilder.build());

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "multipart/form-data"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
    }

    @Test
    public void testCompressionWithBodyMultiPartFormPut() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        final String payload = "This is a mock payload.";
        multipartBuilder.addFormDataPart("string_prop", payload);
        final RequestBuilder builder = RequestBuilder.put(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.body(multipartBuilder.build());

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "multipart/form-data"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(200, response.getStatusCode());


        // Verify the request
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "PUT");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
    }

    @Test
    public void testShouldNotGzipCompressWithBodyJsonObject() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final TestModel model = new TestModel();
        model.setSuccess("awesome");

        final JsonObject contentJson = new JsonObject();
        contentJson.addProperty("success", model.getSuccess());

        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Accept", "application/json");
        builder.header("Content-Encoding", "deflate");
        builder.bodyJson(contentJson).build();

        // queue the response to the mock server
        String mockResponseBody = contentJson.toString();
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "application/json"));

        // validate response
        Response<TestModel> response = service.testMethod(builder).execute();
        assertNotNull(response);
        TestModel responseObj = response.getResult();
        assertNotNull(responseObj);
        assertEquals(201, response.getStatusCode());
        assertEquals("awesome", responseObj.getSuccess());

        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("deflate", request.getHeader(CONTENT_ENCODING));
        assertEquals(mockResponseBody.length(), request.getBodySize());
    }

    @Test
    public void testShouldNotGzipCompressBodyOctetStreamPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String mockPayload = "This is a mock file.";
        final InputStream payload = new ByteArrayInputStream(mockPayload.getBytes());
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Content-Encoding", "deflate");
        builder.bodyContent(payload, "application/octet-stream").build();

        // queue the response to the mock server
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-type", "application/octet-stream"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());

        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertTrue(request.getBodySize() > 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals("deflate", request.getHeader(CONTENT_ENCODING));
    }

    @Test
    public void testShouldNotGzipCompressBodyTextPlainPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Content-Encoding", "deflate");
        builder.bodyContent(payload, "text/plain").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/plain"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("deflate", request.getHeader(CONTENT_ENCODING));
        assertEquals(payload.length(), request.getBodySize());
    }

    @Test
    public void testShouldNotGzipCompressBodyTextHtmlPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Content-Encoding", "deflate");
        builder.bodyContent(payload, "text/html").build();

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/html"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("deflate", request.getHeader(CONTENT_ENCODING));
        assertEquals(payload.length(), request.getBodySize());
    }

    @Test
    public void testShouldNotGzipCompressBodyMultiPartFormPost() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // build the request
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        final String payload = "This is a mock payload.";
        multipartBuilder.addFormDataPart("string_prop", payload);
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.header("Content-Encoding", "deflate");
        builder.body(multipartBuilder.build());

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "multipart/form-data"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("deflate", request.getHeader(CONTENT_ENCODING));
    }

    @Test
    public void testRetrySuccessWithGzip() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = true;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/plain").build();

        String message = "The request failed because the moon is full.";

        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"error\": \"" + message + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();


        Response<TestModel> r = service.testMethod(builder).execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(2, server.getRequestCount());

        // Verify both requests were compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, ungzipRequestBody(request.getBody()));
        request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, ungzipRequestBody(request.getBody()));
    }

    @Test
    public void testRetrySuccessWithGzipEnabledThenDisabled() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = true;
        setUp(enableGzip, enableRateLimit);
        // build the request
        final String payload = "This is a mock payload.";
        final RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));
        builder.bodyContent(payload, "text/plain").build();

        String message = "The request failed because the moon is full.";

        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"error\": \"" + message + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));

        // the expected compressed request body
        Buffer gzippedBody = gzip(payload.toString());
        long bodySize = gzippedBody.size();


        Response<TestModel> r = service.testMethod(builder).execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(2, server.getRequestCount());

        // Verify both requests were compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, ungzipRequestBody(request.getBody()));
        request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertEquals("gzip", request.getHeader(CONTENT_ENCODING));
        assertEquals("gzip", request.getHeader(ACCEPT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, ungzipRequestBody(request.getBody()));

        // Now disable gzip, verify gzip interceptor was removed, and retry
        // interceptor is still intact
        enableGzip = false;
        service.enableGzipCompression(false);

        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"error\": \"" + message + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .setBody("{\"success\": \"awesome\"}"));

        r = service.testMethod(builder).execute();

        assertEquals(200, r.getStatusCode());
        assertEquals("awesome", r.getResult().getSuccess());
        assertEquals(4, server.getRequestCount());

        bodySize = payload.length();

        // Verify both requests were NOT compressed
        request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, request.getBody().readUtf8());
        request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "POST");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(bodySize, request.getBodySize());
        assertEquals(payload, request.getBody().readUtf8());
    }

    @Test
    public void testShouldNotGzipDeleteZeroLengthBody() throws Throwable {
        boolean enableGzip = true;
        boolean enableRateLimit = false;
        setUp(enableGzip, enableRateLimit);
        // OkHttp DELETE requests appear to have a zero length body (as opposed to a null body)
        final RequestBuilder builder = RequestBuilder.delete(HttpUrl.parse(service.getServiceUrl() + "/v1/test"));

        // queue the response to the mock server
        String mockResponseBody = "";
        server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setBody(mockResponseBody)
            .setHeader("Content-type", "text/plain"));

        // validate response
        Response<Void> response = service.testMethodVoid(builder).execute();
        assertNotNull(response);
        Void responseObj = response.getResult();
        assertNull(responseObj);
        assertEquals(201, response.getStatusCode());


        // Verify the request was not compressed
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals(request.getMethod(), "DELETE");
        assertNull(request.getHeader(CONTENT_ENCODING));
        assertEquals(0, request.getBodySize());
    }
}
