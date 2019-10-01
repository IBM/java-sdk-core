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

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;

import okhttp3.OkHttpClient;
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
    assertTrue(BaseService.isJsonMimeType("application/json"));
    assertTrue(BaseService.isJsonMimeType("application/json; charset=utf-8"));
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
  public void testOverriddenConfigureHttpClient() {
    AnotherTestService svc = new AnotherTestService("AnotherService");
    assertEquals("AnotherService", svc.getName());
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
    assertEquals(60 * 1000, client.connectTimeoutMillis());
    assertEquals(60 * 1000, client.writeTimeoutMillis());
    assertEquals(5 * 60 * 1000, client.readTimeoutMillis());
  }
}