package com.ibm.cloud.sdk.core.test.http.ratelimit;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ratelimit.RateLimitInterceptor;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RateLimitTest extends BaseServiceUnitTest {

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

    private RateLimitTest.TestService service;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.cloud.sdk.core.test.WatsonServiceTest#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        service = new RateLimitTest.TestService(new NoAuthAuthenticator());

        HttpConfigOptions.Builder builder = new HttpConfigOptions.Builder();
        builder.enableRateLimitRetry(new NoAuthAuthenticator(),1,3);
        service.configureClient(builder.build());
        service.setServiceUrl(getMockWebServerUrl());
    }

    /**
     * Test that we don't endlessly retry on 429
     */
    @Test
    public void testRetryIsExhausted() {

        String message = "The request failed because the moon is full.";
        for (int i = 0; i <= 3; i++) {
            server.enqueue(new MockResponse()
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
     * Test that we retry on 429
     */
    @Test
    public void testRetrySuccess() {

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

    }

    /**
     * Test that we take care of invalid Retry-After headers
     */
    @Test(timeout = 500)
    public void testRetryAfter() {

        String message = "The request failed because the moon is full.";
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
                .addHeader("Retry-After", "-1")
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
}
