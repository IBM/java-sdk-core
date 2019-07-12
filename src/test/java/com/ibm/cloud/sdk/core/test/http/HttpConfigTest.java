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
        .build();

    assertEquals(true, configOptions.shouldDisableSslVerification());
    assertEquals(authenticator, configOptions.getProxyAuthenticator());
    assertEquals(proxy, configOptions.getProxy());
  }
}
