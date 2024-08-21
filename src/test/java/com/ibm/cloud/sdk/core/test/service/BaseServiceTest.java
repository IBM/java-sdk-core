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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.gzip.GzipRequestInterceptor;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.internal.tls.OkHostnameVerifier;

/**
 * Unit tests associated with the BaseService core class.
 *
 */
public class BaseServiceTest {

  // Simulated generated service class.
  public class TestService extends BaseService {
    public TestService(String name) {
      super(name, new NoAuthAuthenticator());
    }
  }

  // A second simulated generated service class.
  public class AnotherTestService extends BaseService {
    public AnotherTestService(String name) {
      super(name, new NoAuthAuthenticator());
    }

    @Override
    protected OkHttpClient configureHttpClient() {
      // Get a client with a default configuration.
      OkHttpClient client = HttpClientSingleton.getInstance().createHttpClient();

      // Create a variation of the default client with the read timeout set to 5 minutes.
      OkHttpClient newClient = client.newBuilder()
          .readTimeout(5, TimeUnit.MINUTES)
          .build();

      return newClient;
    }
  }

  @Test
  public void testMimeTypes() {
    assertFalse(BaseService.isJsonMimeType(null));
    assertFalse(BaseService.isJsonPatchMimeType(null));
    assertTrue(BaseService.isJsonMimeType("application/json"));
    assertTrue(BaseService.isJsonMimeType("application/json; charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("application/json ;charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("application/json;charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("APPLICATION/JSON;charset=utf-16"));
    assertFalse(BaseService.isJsonMimeType("application/notjson"));
    assertFalse(BaseService.isJsonMimeType("application/json-patch+json"));
    assertFalse(BaseService.isJsonMimeType("APPlication/JSON-patCH+jSoN;charset=utf-8"));
    assertTrue(BaseService.isJsonPatchMimeType("APPlication/JSON-patCH+jSoN;charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("application/merge-patch+json"));
    assertTrue(BaseService.isJsonMimeType("application/merge-patch+json;charset=utf-8"));
    assertFalse(BaseService.isJsonMimeType("application/json2-patch+json"));
    assertFalse(BaseService.isJsonMimeType("application/merge-patch+json-blah"));
    assertFalse(BaseService.isJsonMimeType("application/merge patch json"));

    assertTrue(BaseService.isJsonPatchMimeType("application/json-patch+json"));
    assertTrue(BaseService.isJsonPatchMimeType("application/json-patch+json;charset=utf-8"));
    assertTrue(BaseService.isJsonPatchMimeType("application/json-patch+json ; charset=utf-8"));
    assertFalse(BaseService.isJsonPatchMimeType("application/json"));
    assertFalse(BaseService.isJsonPatchMimeType("APPLICATION/JsOn; charset=utf-8"));
    assertFalse(BaseService.isJsonPatchMimeType("application/merge-patch+json"));
    assertFalse(BaseService.isJsonPatchMimeType("application/merge-patch+json;charset=utf-8"));
  }

  @Test
  public void testDefaultHttpClient() {
    TestService svc = new TestService("MyService");
    assertEquals("MyService", svc.getName());
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
    assertEquals(60 * 1000, client.connectTimeoutMillis());
    assertEquals(60 * 1000, client.writeTimeoutMillis());
    assertEquals(90 * 1000, client.readTimeoutMillis());
    assertNotNull(client.hostnameVerifier());
    assertTrue(client.hostnameVerifier() instanceof OkHostnameVerifier);
  }

  @Test
  public void testConfigureClient() {
    TestService svc = new TestService("MyService");
    assertEquals("MyService", svc.getName());
    OkHttpClient origClient = svc.getClient();
    assertTrue(origClient.hostnameVerifier() instanceof OkHostnameVerifier);

    // Disable ssl verification, which should result in a NEW client instance.
    HttpConfigOptions options = new HttpConfigOptions.Builder()
        .disableSslVerification(true)
        .build();
    svc.configureClient(options);

    // Verify the new client instance.
    OkHttpClient client = svc.getClient();
    assertNotEquals(origClient, client);
    assertFalse(client.hostnameVerifier() instanceof OkHostnameVerifier);
    assertEquals(origClient.connectionPool(), client.connectionPool());
  }

  @Test
  public void testConfigureClientGzipEnabled() {
    TestService svc = new TestService("MyService");
    assertEquals("MyService", svc.getName());
    OkHttpClient origClient = svc.getClient();
    assertTrue(origClient.hostnameVerifier() instanceof OkHostnameVerifier);

    // Enable gzip compression, which should result in a NEW client instance.
    HttpConfigOptions options = new HttpConfigOptions.Builder()
        .enableGzipCompression(true)
        .build();
    svc.configureClient(options);

    // Verify the new client instance.
    OkHttpClient client = svc.getClient();
    assertNotEquals(origClient, client);;
    List<Interceptor> interceptors = client.interceptors();
    assertTrue(interceptors.size() > 0);

    boolean containsGzipInterceptor = false;
    GzipRequestInterceptor gzip = new GzipRequestInterceptor();

    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertTrue(containsGzipInterceptor);

    // Now disable ssl, this should result in a new client with gzip STILL enabled
    options = new HttpConfigOptions.Builder()
        .disableSslVerification(true)
        .build();
    svc.configureClient(options);

    // Verify the new client instance.
    client = svc.getClient();
    interceptors = client.interceptors();
    assertNotEquals(origClient, client);
    assertFalse(client.hostnameVerifier() instanceof OkHostnameVerifier);
    assertEquals(origClient.connectionPool(), client.connectionPool());
    assertTrue(interceptors.size() > 0);

    containsGzipInterceptor = false;

    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertTrue(containsGzipInterceptor);
  }

  @Test
  public void testOverriddenConfigureHttpClient() {
    AnotherTestService svc = new AnotherTestService("AnotherService");
    assertEquals("AnotherService", svc.getName());
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
    assertEquals(60 * 1000, client.connectTimeoutMillis());
    assertEquals(60 * 1000, client.writeTimeoutMillis());
    assertEquals(5 * 60 * 1000, client.readTimeoutMillis());
  }

  @Test
  public void testCreateServiceCallOverride() {
    class ExtendingService extends BaseService {

      @Override
      protected <T> ServiceCall<T> createServiceCall(final Request request, final ResponseConverter<T> converter) {
        // For test purposes override to just return null
        return null;
      }

      public ServiceCall<String> testOperation() {
        Request r = new Request.Builder().url("https://foo.example").get().build();
        return createServiceCall(r, ResponseConverterUtils.getString());
      }
    };
    ExtendingService extendingService = new ExtendingService();
    ServiceCall<String> testCall = extendingService.testOperation();
    // Assert that the override was in place
    assertNull(testCall, "The service call should have been overridden to return null.");
  }

  @Test
  public void testToString() {
    TestService svc = new TestService("test");
    assertNotNull(svc.toString());
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testSetServiceUrlError() {
    TestService svc = new TestService("test");
    svc.setServiceUrl("{https://badurl}");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testSetServiceUrl() {
    String mockUrl = "https://myservice.com/api";
    TestService svc = new TestService("test");
    svc.setServiceUrl(mockUrl);
    assertEquals(svc.getServiceUrl(), mockUrl);

    svc.setEndPoint(mockUrl);
    assertEquals(svc.getEndPoint(), mockUrl);
  }

  @Test
  public void testSetClient() {
    TestService svc = new TestService("test");
    OkHttpClient client = HttpClientSingleton.getInstance().createHttpClient();
    svc.setClient(client);
    assertEquals(svc.getClient(), client);
  }

  @Test
  public void testSetDefaultHeaders() {
    Map<String, String> rawHeaders = new HashMap<>();
    rawHeaders.put("header1", "value1");
    Headers expectedHeaders = Headers.of(rawHeaders);

    TestService svc = new TestService("test");
    assertNull(svc.getDefaultHeaders());

    svc.setDefaultHeaders(rawHeaders);
    assertNotNull(svc.getDefaultHeaders());
    assertEquals(svc.getDefaultHeaders(), expectedHeaders);
  }
}