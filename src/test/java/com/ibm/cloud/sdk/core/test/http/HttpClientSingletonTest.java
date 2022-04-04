/**
 * (C) Copyright IBM Corp. 2022.
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

package com.ibm.cloud.sdk.core.test.http;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.IRetryInterceptor;
import com.ibm.cloud.sdk.core.http.IRetryStrategy;
import com.ibm.cloud.sdk.core.http.RetryInterceptor;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;

import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * Unit tests for the HttpClientSingleton class.
 */
public class HttpClientSingletonTest {

    private void getAndAssertEnabledProtocols(OkHttpClient client) throws IOException {
        SSLSocket sslSocket = (SSLSocket) client.sslSocketFactory().createSocket();
        List<String> enabledProtocols = Arrays.asList(sslSocket.getEnabledProtocols());
        // Assert there is at least 1 enabled protocol
        assertTrue(enabledProtocols.size() > 0, "There should be at least 1 TLS protocol enabled.");

        // Get the MODERN_TLS Java names and the runtime supported protocols
        List<String> modernTlsNames = new ArrayList<>();
        for (TlsVersion tlsVersion : ConnectionSpec.MODERN_TLS.tlsVersions()) {
            modernTlsNames.add(tlsVersion.javaName());
        }
        List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
        // Iterate and assert that each enabled protocol is present in both lists
        for (String protocol : enabledProtocols) {
            // Assert that the enabled protocols is supported by the runtime
            assertTrue(supportedProtocols.contains(protocol),
                String.format("The enabled protocol %s should be supported by the runtime", protocol));
            // Assert that the enabled protocols is present in MODERN_TLS
            assertTrue(modernTlsNames.contains(protocol),
                String.format("The enabled protocol %s should be in the MODERN_TLS connection spec", protocol));
        }
    }


    @Test
    public void testTlsProtocolFiltering() throws IOException {
        OkHttpClient client = HttpClientSingleton.getInstance().createHttpClient();
        getAndAssertEnabledProtocols(client);
    }

    @Test
    public void testTlsProtocolFilteringWithVerificationDisabled() throws IOException {
        HttpConfigOptions configOptions = new HttpConfigOptions.Builder()
            .disableSslVerification(true)
            .build();
        OkHttpClient client = HttpClientSingleton.getInstance().configureClient(configOptions);
        getAndAssertEnabledProtocols(client);
    }

    // Simulated user-defined retry interceptor implementation.
    public static class TestRetryInterceptor extends RetryInterceptor {
      public TestRetryInterceptor(int maxRetries, int maxRetryInterval, Authenticator authenticator) {
        super(maxRetries, maxRetryInterval, authenticator);
      }
    }

    // Simulated user-defined retry interceptor factory.
    public static class TestRetryStrategy implements IRetryStrategy {
      @Override
      public IRetryInterceptor createRetryInterceptor(int maxRetries, int maxRetryInterval,
          Authenticator authenticator) {
        return new TestRetryInterceptor(maxRetries, maxRetryInterval, authenticator);
      }
    }

    @Test
    public void testSetRetryStrategy() {
      // Register our factory.
      HttpClientSingleton.setRetryStrategy(new TestRetryStrategy());

      // Create a client with retries enabled.
      HttpConfigOptions options = new HttpConfigOptions.Builder().enableRetries(new NoAuthAuthenticator(), 5, 60)
          .build();
      OkHttpClient client = HttpClientSingleton.getInstance().configureClient(options);

      // Now verify that our retry interceptor is registered on the client instance.
      int testRetryInterceptors = 0;
      int otherRetryInterceptors = 0;
      OkHttpClient.Builder builder = client.newBuilder();
      if (!builder.interceptors().isEmpty()) {
        for (Iterator<Interceptor> iter = builder.interceptors().iterator(); iter.hasNext();) {
          Interceptor element = iter.next();
          if (element instanceof TestRetryInterceptor) {
            testRetryInterceptors++;
          } else if (IRetryInterceptor.class.isAssignableFrom(element.getClass())) {
            otherRetryInterceptors++;
          }
        }
      }
      assertEquals(testRetryInterceptors, 1);
      assertEquals(otherRetryInterceptors, 0);
    }

    @Test
    public void testSetClient() {

      // Create a custom client and set it on the HttpClientSingleton
      // as the default for configuring other clients.
      ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
          .allEnabledCipherSuites()
          .build();
      OkHttpClient client = new OkHttpClient.Builder()
          .connectTimeout(30, TimeUnit.SECONDS)
          .writeTimeout(120, TimeUnit.SECONDS)
          .readTimeout(120, TimeUnit.SECONDS)
          .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
          .build();
      HttpClientSingleton cs = HttpClientSingleton.getInstance();
      assertNotNull(cs.getHttpClient());

      // Verify set/get.
      cs.setHttpClient(client);
      assertEquals(cs.getHttpClient(), client);

      // Verify that "client" is used to configure other clients.
      // To verify this, we'll just configure a new client with SSL verification disabled,
      // and its timeout properties should be the same as what we set above on our original client.
      HttpConfigOptions options = new HttpConfigOptions.Builder()
          .disableSslVerification(true)
          .build();
      OkHttpClient client2 = cs.configureClient(options);

      // Verify that the "configureClient()" call above set a new default client instance in the singleton.
      assertNotEquals(cs.getHttpClient(), client);

      // Verify that the new client is a different instance than our original client.
      assertNotEquals(client2, client);

      // Verify that the new client "inherits" the config from our original client.
      assertEquals(client2.connectTimeoutMillis(), 30 * 1000);
      assertEquals(client2.writeTimeoutMillis(), 120 * 1000);
      assertEquals(client2.readTimeoutMillis(), 120 * 1000);
    }
  }
