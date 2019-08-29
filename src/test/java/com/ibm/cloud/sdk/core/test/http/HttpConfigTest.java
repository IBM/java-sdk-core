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

package com.ibm.cloud.sdk.core.test.http;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.junit.Test;

import com.ibm.cloud.sdk.core.http.HttpConfigOptions;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the HttpConfigOptions object.
 */
public class HttpConfigTest {

  @Test
  public void testHttpConfigOptions() {
    Authenticator authenticator = new Authenticator() {
      @Nullable
      @Override
      public Request authenticate(@Nullable Route route, Response response) throws IOException {
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

    assertEquals(true, configOptions.shouldDisableSslVerification());
    assertEquals(authenticator, configOptions.getProxyAuthenticator());
    assertEquals(proxy, configOptions.getProxy());
    assertEquals(HttpConfigOptions.LoggingLevel.HEADERS, configOptions.getLoggingLevel());
  }
}
