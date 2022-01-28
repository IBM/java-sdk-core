/**
 * (C) Copyright IBM Corp. 2015, 2022.
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

import static com.ibm.cloud.sdk.core.test.TestUtils.getStringFromInputStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.RetryInterceptor;
import com.ibm.cloud.sdk.core.http.gzip.GzipRequestInterceptor;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Unit tests associated with the BaseService core class.
 *
 */
public class ConfigureServiceTest {
  private static final String ALTERNATE_CRED_FILENAME = "src/test/resources/my-credentials.env";
  private static final String VCAP_SERVICES = "vcap_services.json";

  // Simulated generated service class.
  public class TestService extends BaseService {
    public TestService(String name, Authenticator authenticator) {
      super(name, authenticator);
    }
  }

  public class TestServiceConfigured extends BaseService {
    public TestServiceConfigured(String name, Authenticator authenticator) {
      super(name, authenticator);
      this.configureService(name);
    }
  }

  /**
   * Setup.
   */
  public void setupVCAP() {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(VCAP_SERVICES);
    final String vcapServices = getStringFromInputStream(in);

    System.setProperty("VCAP_SERVICES", vcapServices);
    clearSystemProp();
  }

  public void clearVCAP() {
    System.clearProperty("VCAP_SERVICES");
  }

  @BeforeMethod
  public void setSystemProp() {
    System.setProperty("IBM_CREDENTIALS_FILE", ALTERNATE_CRED_FILENAME);
  }

  @AfterMethod
  public void clearSystemProp() {
    System.clearProperty("IBM_CREDENTIALS_FILE");
  }

  @Test
  public void testUnConfigureServiceOnInitiationCreds() {
    TestService svc = new TestService("SERVICE_1", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());
  }

  @Test
  public void testConfigureServiceOnInitiationCreds() {
    TestServiceConfigured svc = new TestServiceConfigured("SERVICE_1", new NoAuthAuthenticator());
    assertEquals(svc.getServiceUrl(), "https://service1/api");

    OkHttpClient client = svc.getClient();
    assertNotNull(client);
    List<Interceptor> interceptors = client.interceptors();
    assertTrue(interceptors.size() == 0);
  }

  @Test
  public void testConfigureServiceOnInitiationCredsGzipEnabled() {
    TestServiceConfigured svc = new TestServiceConfigured("SERVICE_9", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());

    // Confirm gzip was enabled in the client
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
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

    // Manually call enable gzip and confirm the interceptor is still present
    svc.enableGzipCompression(true);
    containsGzipInterceptor = false;
    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertTrue(containsGzipInterceptor);

    // Disable gzip and validate it was removed from the client
    containsGzipInterceptor = false;
    svc.enableGzipCompression(false);
    client = svc.getClient();
    assertNotNull(client);
    interceptors = client.interceptors();

    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertFalse(containsGzipInterceptor);
  }

  @Test
  public void testConfigureServiceOnInitiationCredsGzipDisabled() {
    TestServiceConfigured svc = new TestServiceConfigured("SERVICE_10", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());

    // Confirm gzip was not enabled in the client
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
    List<Interceptor> interceptors = client.interceptors();
    assertFalse(interceptors.size() > 0);

    boolean containsGzipInterceptor = false;
    GzipRequestInterceptor gzip = new GzipRequestInterceptor();

    // Manually call enable gzip and confirm the interceptor is present
    svc.enableGzipCompression(true);
    containsGzipInterceptor = false;
    client = svc.getClient();
    interceptors = client.interceptors();
    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertTrue(containsGzipInterceptor);
  }

  @Test
  public void testConfigureServiceAfterInitiationCredsGzipEnabled() {
    TestService svc = new TestService("SERVICE_1", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());

    // Enable gzip
    svc.enableGzipCompression(true);
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
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

    // Manually call configureSvc and confirm gzip is still enabled
    svc.configureService("SERVICE_1");
    containsGzipInterceptor = false;
    client = svc.getClient();
    interceptors = client.interceptors();
    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertTrue(containsGzipInterceptor);
  }

  @Test
  public void testConfigureServiceAfterInitiationCredsGzipDisabled() {
    TestService svc = new TestService("SERVICE_10", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());

    // Enable gzip
    svc.enableGzipCompression(true);
    OkHttpClient client = svc.getClient();
    assertNotNull(client);
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

    // Manually call configureSvc and confirm gzip was disabled
    svc.configureService("SERVICE_10");
    containsGzipInterceptor = false;
    client = svc.getClient();
    interceptors = client.interceptors();
    for (Interceptor is: interceptors) {
      if (is.getClass().equals(gzip.getClass())) {
        containsGzipInterceptor = true;
       }
    }
    assertFalse(containsGzipInterceptor);
  }

  @Test
  public void testConfigureServiceRetriesEnabled() {
    TestService svc = new TestService("test", new NoAuthAuthenticator());
    svc.configureService("SERVICE_14");
    boolean containsRetryInterceptor = false;
    OkHttpClient client = svc.getClient();
    List<Interceptor> interceptors = client.interceptors();
    for (Interceptor is: interceptors) {
      if (is.getClass().equals(RetryInterceptor.class)) {
        containsRetryInterceptor = true;
       }
    }
    assertTrue(containsRetryInterceptor);
  }

  @Test
  public void testConfigureServiceOnInitiationSystemPropFile() {
    TestServiceConfigured svc = new TestServiceConfigured("SERVICE_1", new NoAuthAuthenticator());
    assertEquals(svc.getServiceUrl(), "https://service1/api");
  }

  @Test
  public void testUnConfigureServiceOnInitiationVcap() {
    setupVCAP();

    TestService svc = new TestService("key_to_service_entry_2", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());

    clearVCAP();
  }

  @Test
  public void testConfigureServiceOnInitiationVcap() {
    setupVCAP();

    TestServiceConfigured svc =
        new TestServiceConfigured("service_entry_key_and_key_to_service_entries", new NoAuthAuthenticator());
    assertEquals(svc.getServiceUrl(), "https://on.the.toolchainplatform.net/devops-insights/api");

    clearVCAP();
  }

  @Test
  public void testConfigureServiceViaCredFile() {
    TestService svc = new TestService("SERVICE_1", new NoAuthAuthenticator());
    assertNull(svc.getServiceUrl());
    svc.configureService("SERVICE_1");
    assertEquals(svc.getServiceUrl(), "https://service1/api");
  }

  @Test
  public void testConfigureServiceViaVcap() {
    setupVCAP();

    TestService svc = new TestService("discovery", new NoAuthAuthenticator());
    assertEquals(null, svc.getServiceUrl());
    svc.configureService("discovery");
    assertEquals(svc.getServiceUrl(), "https://api.us-south.discovery-experimental.watson.cloud.ibm.com");

    clearVCAP();
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testConfigureServiceFailure1() {
    TestService svc = new TestService("test", new NoAuthAuthenticator());
    svc.configureService("");
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testConfigureServiceFailure2() {
    TestService svc = new TestService("test", new NoAuthAuthenticator());
    svc.configureService(null);
  }
}