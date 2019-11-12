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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.util.EnvironmentUtils;
import static com.ibm.cloud.sdk.core.test.TestUtils.getStringFromInputStream;

import java.io.InputStream;

/**
 * Unit tests associated with the BaseService core class.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EnvironmentUtils.class })
@PowerMockIgnore("javax.net.ssl.*")
public class ConfigureServiceTest {
  private static final String ALTERNATE_CRED_FILENAME = "src/test/resources/my-credentials.env";
  private static final String VCAP_SERVICES = "vcap_services.json";
  private static final String BASIC_USERNAME = "basicUser";
  // Simulated generated service class.
  public class TestService extends BaseService {
    public TestService(String name, Authenticator authenticator) {
      super(name, authenticator);
    }
    public void configureSvc(String serviceName) {
        this.configureService(serviceName);
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

    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("VCAP_SERVICES")).thenReturn(vcapServices);
  }

  @Test
  public void testUnConfigureServiceOnInitiationCreds() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService svc = new TestService("SERVICE_1", auth);
    assertNull(svc.getServiceUrl());
  }

  @Test
  public void testConfigureServiceOnInitiationCreds() {
    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestServiceConfigured svc = new TestServiceConfigured("SERVICE_1", auth);
    assertEquals("https://service1/api", svc.getServiceUrl());
  }

  @Test
  public void testUnConfigureServiceOnInitiationVcap() {
    setupVCAP();
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService svc = new TestService("key_to_service_entry_2", auth);
    assertNull(svc.getServiceUrl());
  }

  @Test
  public void testConfigureServiceOnInitiationVcap() {
    setupVCAP();
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestServiceConfigured svc = new TestServiceConfigured("service_entry_key_and_key_to_service_entries", auth);
    assertEquals("https://on.the.toolchainplatform.net/devops-insights/api", svc.getServiceUrl());
  }

  @Test
  public void testConfigureServiceViaCredFile() {
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService svc = new TestService("SERVICE_1", auth);
    assertEquals(null, svc.getServiceUrl());

    PowerMockito.spy(EnvironmentUtils.class);
    PowerMockito.when(EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE")).thenReturn(ALTERNATE_CRED_FILENAME);
    svc.configureSvc("SERVICE_1");
    assertEquals("https://service1/api", svc.getServiceUrl());
  }

  @Test
  public void testConfigureServiceViaVcap() {
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService svc = new TestService("discovery", auth);
    assertEquals(null, svc.getServiceUrl());

    setupVCAP();
    svc.configureSvc("discovery");
    assertEquals("https://gateway.watsonplatform.net/discovery-experimental/api", svc.getServiceUrl());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigureServiceFailure() {
    BasicAuthenticator auth = new BasicAuthenticator(BASIC_USERNAME, "password1");
    TestService svc = new TestService("discovery", auth);
    svc.configureSvc("");
  }
}