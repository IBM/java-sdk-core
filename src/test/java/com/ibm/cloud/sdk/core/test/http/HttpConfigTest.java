/**
 * (C) Copyright IBM Corp. 2015, 2023.
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.testng.annotations.Test;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.util.HttpLogging;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Unit tests for the HttpConfigOptions object.
 */
@SuppressWarnings("deprecation")
public class HttpConfigTest {

  @Test
  public void testHttpConfigOptions() {
    Authenticator authenticator = new Authenticator() {
      @Override
      public Request authenticate(Route route, Response response) throws IOException {
        return null;
      }
    };

    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxyHost", 8080));
    HttpConfigOptions configOptions = new HttpConfigOptions.Builder()
        .disableSslVerification(true)
        .proxy(proxy)
        .proxyAuthenticator(authenticator)
        .loggingLevel(HttpConfigOptions.LoggingLevel.HEADERS)
        .build();

    assertTrue(configOptions.shouldDisableSslVerification());
    assertEquals(authenticator, configOptions.getProxyAuthenticator());
    assertNull(configOptions.getGzipCompression());
    assertEquals(Boolean.TRUE, configOptions.getCustomRedirects());
    assertEquals(proxy, configOptions.getProxy());
    assertEquals(HttpConfigOptions.LoggingLevel.HEADERS, configOptions.getLoggingLevel());

    OkHttpClient client = HttpClientSingleton.getInstance().configureClient(configOptions);
    assertNotNull(client);

    // Call configureClient to cover some additional statements.
    client = HttpClientSingleton.getInstance().configureClient(configOptions);
    assertNotNull(client);

    client = HttpClientSingleton.getInstance().configureClient(null);
    assertNotNull(client);
  }


  @Test
  public void testHttpLogging() {
    HttpLoggingInterceptor interceptor = HttpLogging.getLoggingInterceptor();
    assertNotNull(interceptor);
  }

}
