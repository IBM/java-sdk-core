/**
 * (C) Copyright IBM Corp. 2023.
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

package com.ibm.cloud.sdk.core.test.http.redirect;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static com.ibm.cloud.sdk.core.http.HttpHeaders.LOCATION;
import static org.junit.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static java.util.Map.entry;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RedirectInterceptor;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BearerTokenAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.exception.UnauthorizedException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class RedirectTest extends BaseServiceUnitTest {
    private static final Logger LOG = Logger.getLogger(RedirectTest.class.getName());
    private static final String OPERATION_PATH = "/v1/path";

    // Logging level used by this test.
    // For debugging, set this to Level.FINE or Level.ALL, etc.
    private static Level logLevel = Level.SEVERE;

    // This will be our mocked version of the EnvironmentUtils class.
    private static MockedStatic<EnvironmentUtils> envMock = null;

    public void createEnvMock() {
      envMock = Mockito.mockStatic(EnvironmentUtils.class);
    }

    public void clearEnvMock() {
      if (envMock != null) {
        envMock.close();
        envMock = null;
      }
    }

    public class TestModel extends GenericModel {
        String name;

        public String getName() {
            return name;
        }
    }

    public class TestService extends BaseService {

        private static final String SERVICE_NAME = "test";

        TestService(Authenticator auth) {
            super(SERVICE_NAME, auth);
        }

        ServiceCall<TestModel> testGet() {
            RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + OPERATION_PATH));
            return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
        }

        ServiceCall<TestModel> testPost() {
            RequestBuilder builder = RequestBuilder.post(HttpUrl.parse(getServiceUrl() + OPERATION_PATH));
            builder.bodyContent("this is the request body", "text/plain");
            return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
        }
    }

    private RedirectTest.TestService service;
    protected MockWebServer server2;

    protected Map<String, String> defaultHeaders = Map.ofEntries(
        entry("WWW-Authenticate", "authentication instructions"),
        entry("Cookie", "chocolate chip"),
        entry("Cookie2", "snickerdoodle"),
        entry("Foo", "bar"));

    protected List<String> allHeaders = Arrays.asList("Authorization", "WWW-Authenticate", "Cookie", "Cookie2", "Foo");
    protected List<String> unsafeHeaders = Arrays.asList("Foo");

    /**
     * Sets up the two mock servers and the mock service instance prior to the
     * invocation of each test method.
     */
    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        server2 = new MockWebServer();
        server2.start();

        service = new RedirectTest.TestService(new BearerTokenAuthenticator("this is not a secret"));
        service.setServiceUrl(getServerUrl1());
        service.setDefaultHeaders(defaultHeaders);

        createEnvMock();

        // Set up java.util.logging to display messages on the console.
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(logLevel);
        Logger logger;
        logger = Logger.getLogger(RedirectInterceptor.class.getName());
        logger.setLevel(logLevel);
        logger.addHandler(handler);

        logger = Logger.getLogger(HttpClientSingleton.class.getName());
        logger.setLevel(logLevel);
        logger.addHandler(handler);

        LOG.setLevel(logLevel);
        LOG.addHandler(handler);
    }

    @Override
    @AfterMethod
    public void tearDown() throws IOException {
        super.tearDown();
        server2.shutdown();
        clearEnvMock();
    }

    protected String getServerUrl1() {
        return super.getMockWebServerUrl();
    }

    protected String getServerUrl2() {
        return StringUtils.chop(server2.url("/").toString());
    }

    /**
     * Generic test method that simulates a redirected request scenario for an
     * initial GET request.
     *
     * @param host1          the "first" host to which the original request is to be sent
     * @param host2          the "second" host to which the redirected request is to be sent
     * @param headersPresent the set of headers that should be present in the redirected request
     * @throws Exception
     */
    protected void runRedirectTestGet(String host1, String host2, List<String> headersPresent, int expectedStatusCode,
            int redirectStatusCode) throws Exception {
        String server1Url = getServerUrl1().replace("localhost", host1);
        String server2Url = getServerUrl2().replace("localhost", host2);
        String location = server2Url + OPERATION_PATH;
        server.enqueue(new MockResponse().setResponseCode(redirectStatusCode).addHeader(LOCATION, location));
        if (expectedStatusCode == 200) {
            server2.enqueue(new MockResponse().setResponseCode(200)
                    .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON).setBody("{\"name\": \"Jason Bourne\"}"));
        } else {
            server2.enqueue(new MockResponse().setResponseCode(401));
        }

        service.setServiceUrl(server1Url);
        Response<TestModel> r = null;
        try {
            r = service.testGet().execute();
            assertEquals(expectedStatusCode, r.getStatusCode());
        } catch (UnauthorizedException e) {
            assertEquals(expectedStatusCode, 401);
        } catch (Throwable t) {
            fail("Caught unexpected exception: " + t.toString());
        }

        if (expectedStatusCode == 200) {
            assertNotNull(r);
            assertEquals(r.getResult().getName(), "Jason Bourne");
        }

        // Make sure the first request has all the headers.
        assertEquals(server.getRequestCount(), 1);
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), OPERATION_PATH);
        verifyHeadersPresent(request, allHeaders);

        // Make sure the second request has only the headers contained in
        // "headersPresent".
        assertEquals(server2.getRequestCount(), 1);
        request = server2.takeRequest();
        assertEquals(request.getPath(), OPERATION_PATH);
        assertEquals(request.getMethod(), "GET");
        verifyHeadersPresent(request, headersPresent);
        List<String> headersAbsent = new ArrayList<>(allHeaders);
        headersAbsent.removeAll(headersPresent);
        verifyHeadersAbsent(request, headersAbsent);
    }

    /**
     * Generic test method that simulates a redirected request scenario for an
     * initial POST request with a body.
     *
     * @param host1          the "first" host to which the original request is to be sent
     * @param host2          the "second" host to which the redirected request is to be sent
     * @param headersPresent the set of headers that should be present in the redirected request
     * @throws Exception
     */
    protected void runRedirectTestPost(String host1, String host2, List<String> headersPresent, int expectedStatusCode,
            int redirectStatusCode) throws Exception {
        String server1Url = getServerUrl1().replace("localhost", host1);
        String server2Url = getServerUrl2().replace("localhost", host2);
        String location = server2Url + OPERATION_PATH;
        server.enqueue(new MockResponse().setResponseCode(redirectStatusCode).addHeader(LOCATION, location));
        if (expectedStatusCode == 200) {
            server2.enqueue(new MockResponse().setResponseCode(200)
                    .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON).setBody("{\"name\": \"Jason Bourne\"}"));
        } else {
            server2.enqueue(new MockResponse().setResponseCode(401));
        }

        service.setServiceUrl(server1Url);
        Response<TestModel> r = null;
        try {
            r = service.testPost().execute();
            assertEquals(expectedStatusCode, r.getStatusCode());
        } catch (UnauthorizedException e) {
            assertEquals(expectedStatusCode, 401);
        } catch (Throwable t) {
            fail("Caught unexpected exception: " + t.toString());
        }

        if (expectedStatusCode == 200) {
            assertNotNull(r);
            assertEquals(r.getResult().getName(), "Jason Bourne");
        }

        // Make sure the first request has all the headers.
        assertEquals(server.getRequestCount(), 1);
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), OPERATION_PATH);
        verifyHeadersPresent(request, allHeaders);

        // Make sure the second request has only the headers contained in
        // "headersPresent".
        assertEquals(server2.getRequestCount(), 1);
        request = server2.takeRequest();
        assertEquals(request.getPath(), OPERATION_PATH);
        // Also make sure the redirected request was mapped to a GET, depending on the
        // redirect status code used.
        if (redirectStatusCode == 307 || redirectStatusCode == 308) {
            assertEquals(request.getMethod(), "POST");
            assertTrue(request.getBody().size() > 0);
        } else {
            assertEquals(request.getMethod(), "GET");
            assertEquals(request.getBody().size(), 0);
        }
        verifyHeadersPresent(request, headersPresent);
        List<String> headersAbsent = new ArrayList<>(allHeaders);
        headersAbsent.removeAll(headersPresent);
        verifyHeadersAbsent(request, headersAbsent);
    }

    @Test
    public void testRedirectAuthSuccessGet1() throws Exception {
        // Hosts are the same.
        runRedirectTestGet("region1.cloud.ibm.com", "region1.cloud.ibm.com", allHeaders, 200, 301);
    }

    @Test
    public void testRedirectAuthSuccessGet2() throws Exception {
        // Hosts are different, but within the safe zone.
        runRedirectTestGet("region1.cloud.ibm.com", "region2.cloud.ibm.com", allHeaders, 200, 300);
    }

    @Test
    public void testRedirectAuthSuccessGet3() throws Exception {
        // Hosts are the same (outside the safe zone, but this is irrelevant).
        runRedirectTestGet("region1.notcloud.ibm.com", "region1.notcloud.ibm.com", allHeaders, 200, 307);
    }

    @Test
    public void testRedirectAuthSuccessPost1() throws Exception {
        // Hosts are the same.
        runRedirectTestPost("region1.cloud.ibm.com", "region1.cloud.ibm.com", allHeaders, 200, 302);
    }

    @Test
    public void testRedirectAuthSuccessPost2() throws Exception {
        // Hosts are different, but within the safe zone.
        runRedirectTestPost("region1.cloud.ibm.com", "region2.cloud.ibm.com", allHeaders, 200, 303);
    }

    @Test
    public void testRedirectAuthSuccessPost3() throws Exception {
        // Hosts are the same (outside of safe zone, but this is irrelevant).
        runRedirectTestPost("region1.notcloud.ibm.com", "region1.notcloud.ibm.com", allHeaders, 200, 308);
    }

    @Test
    public void testRedirectAuthFail1() throws Exception {
        // Hosts different and one is not in safe zone.
        runRedirectTestGet("region1.notcloud.ibm.com", "region2.cloud.ibm.com", unsafeHeaders, 401, 301);
    }

    @Test
    public void testRedirectAuthFail2() throws Exception {
        // Hosts different and one is not in safe zone.
        runRedirectTestGet("region1.cloud.ibm.com", "region2.notcloud.ibm.com", unsafeHeaders, 401, 302);
    }

    @Test
    public void testRedirectAuthFail3() throws Exception {
        // Hosts different and neither are in safe zone.
        runRedirectTestPost("region1.notcloud.ibm.com", "region2.notcloud.ibm.com", unsafeHeaders, 401, 308);
    }

    @Test
    public void testRedirectAuthFail4() throws Exception {
        // Hosts different and neither are in safe zone.
        runRedirectTestPost("region1.notcloud.ibm.com", "region2.notcloud.ibm.com", unsafeHeaders, 401, 303);
    }

    @Test
    public void testRedirectLimit() throws Exception {
        // Tests the limit of 10 redirects.
        // In this scenario, we should throw a RuntimeException with a
        // java.net.ProtocolException as the causedBy.

        String server1Url = getServerUrl1().replace("localhost", "region1.cloud.ibm.com");
        String location = server1Url + OPERATION_PATH;

        // Queue up enough redirect responses to put us over the limit.
        for (int i = 0; i <= 20; i++) {
            server.enqueue(new MockResponse().setResponseCode(301).addHeader(LOCATION, location));
        }
        service.setServiceUrl(server1Url);
        try {
            service.testGet().execute();
            fail("Expected an exception!");
        } catch (RuntimeException e) {
            Throwable causedBy = e.getCause();
            assertNotNull(causedBy);
            assertTrue(causedBy instanceof ProtocolException);
        } catch (Throwable t) {
            fail("Caught incorrect exception: " + t.toString());
        }
    }

    @Test
    public void testRedirectNoCustomRedirects() throws Exception {
        service.configureClient(new HttpConfigOptions.Builder().enableCustomRedirects(false).build());
        assertFalse(checkForInterceptor(service.getClient(), RedirectInterceptor.class));
    }

    @Test
    public void testRedirectBypassInterceptor() throws Exception {
        // Mock the env var to bypass the interceptor.
        envMock.when(() -> EnvironmentUtils.getenv("IBMCLOUD_BYPASS_CUSTOM_REDIRECTS")).thenReturn("true");

        // Trigger the re-config of the service's client instance.
        service.configureClient(null);

        assertFalse(checkForInterceptor(service.getClient(), RedirectInterceptor.class));
    }


    /**
     * Verifies that the headers contained in "headerNames" are present in request
     * "r".
     *
     * @param r           the request to check
     * @param headerNames the names of the headers to verify
     */
    protected void verifyHeadersPresent(RecordedRequest r, List<String> headerNames) {
        for (String h : headerNames) {
            assertNotNull(r.getHeader(h), String.format("Missing header %s from request", h));
        }
    }

    /**
     * Verifies that the headers contained in "headerNames" are not present in
     * request "r".
     *
     * @param r           the request to check
     * @param headerNames the names of the headers that should not be present
     */
    protected void verifyHeadersAbsent(RecordedRequest r, List<String> headerNames) {
        for (String h : headerNames) {
            assertNull(r.getHeader(h), String.format("Header %s should not be present!", h));
        }
    }

    /**
     * Checks to see if "client" contains a registered interceptor with the class "interceptorClass".
     * @param client the client to check
     * @param interceptorClass the interceptor class to look for
     */
    protected boolean checkForInterceptor(OkHttpClient client, Class<?> interceptorClass) {
        assertNotNull(client);
        for (Interceptor i : client.newBuilder().interceptors()) {
            if (interceptorClass.isAssignableFrom(i.getClass())) {
                return true;
            }
        }
        return false;
    }
}
