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

package com.ibm.cloud.sdk.core.http;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.cloud.sdk.core.http.HttpConfigOptions.LoggingLevel;
import com.ibm.cloud.sdk.core.http.gzip.GzipRequestInterceptor;
import com.ibm.cloud.sdk.core.service.security.DelegatingSSLSocketFactory;

import okhttp3.Authenticator;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.TlsVersion;

/**
 * This class encapsulate the {@link OkHttpClient} instance in a singleton pattern. OkHttp performs best when you create
 * a single OkHttpClient instance and reuse it for all of your HTTP calls. This is because each client holds its own
 * connection pool and thread pools. Reusing connections and threads reduces latency and saves memory. Conversely,
 * creating a client for each request wastes resources on idle pools.
 */
public class HttpClientSingleton {
  private static HttpClientSingleton instance = null;

  private static final Logger LOG = Logger.getLogger(HttpClientSingleton.class.getName());

  // retryStrategy serves as a factory for creating retry interceptors.
  private static IRetryStrategy retryStrategy = new DefaultRetryStrategy();

  /**
   * Sets the factory to be used to construct retry interceptor instances.
   * @param strategy the IRetryStrategy implementation to be set as the factory
   * @return the previous factory
   */
  public static synchronized IRetryStrategy setRetryStrategy(IRetryStrategy strategy) {
    IRetryStrategy previousStrategy = retryStrategy;
    retryStrategy = strategy;
    return previousStrategy;
  }

  /**
   * TrustManager for disabling SSL verification, which essentially lets everything through.
   */
  private static final TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws
          CertificateException {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws
          CertificateException {
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
      }
    }
  };

  private static final class FilteredSSLSocketFactory extends DelegatingSSLSocketFactory {

      // Get the TLS version names from the MODERN_TLS connection spec
      private static final List<String> MODERN_TLS_NAMES = new ArrayList<>();

      static {
        for (TlsVersion tlsVersion : ConnectionSpec.MODERN_TLS.tlsVersions()) {
          MODERN_TLS_NAMES.add(tlsVersion.javaName());
        }
        LOG.log(Level.FINEST, "Modern TLS names: {0}", MODERN_TLS_NAMES);
      }

      private FilteredSSLSocketFactory(SSLSocketFactory delegate) {
          super(delegate);
      }

      @Override
      protected SSLSocket configureSocket(SSLSocket socket) throws IOException {
        // Find the TLS protocols supported by this socket
        List<String> supportedTlsNames = Arrays.asList(socket.getSupportedProtocols());
        LOG.log(Level.FINEST, "Socket supported TLS protocols: {0}", supportedTlsNames);
        // Get the union of MODERN_TLS_NAMES and the socket's supported protocols
        List<String> protocolsToEnable = new ArrayList<>();
        protocolsToEnable.addAll(supportedTlsNames);
        protocolsToEnable.retainAll(MODERN_TLS_NAMES);
        LOG.log(Level.FINEST, "Filtered TLS protocols to enable: {0}", protocolsToEnable);
        socket.setEnabledProtocols(protocolsToEnable.toArray(new String[]{}));
        return socket;
      }
  }

  /**
   * Gets the single instance of HttpClientSingleton.
   *
   * @return single instance of HttpClientSingleton
   */
  public static synchronized HttpClientSingleton getInstance() {
    if (instance == null) {
      instance = new HttpClientSingleton();
    }
    return instance;
  }

  /**
   * All new OkHttpClient instances are created from this instance, which contains
   * a default configuration.
   */
  private OkHttpClient okHttpClient;

  /**
   * Instantiates a new HTTP client singleton with a default configuration.
   */
  protected HttpClientSingleton() {
    this.okHttpClient = configureHttpClient();
  }

  /**
   * Returns the current {@link OkHttpClient} instance held by this singleton.
   * This is the client instance that is used as a default configuration from which other client instances are built.
   * @return the current OkHttpClient instance
   */
  public OkHttpClient getHttpClient() {
    return this.okHttpClient;
  }

  /**
   * Sets the current {@link OkHttpClient} instance held by this singleton.
   * This is the client instance that is used as a default configuration from which other client instances are built.
   * @param client the new OkHttpClient instance to use as a default client configuration
   */
  public void setHttpClient(OkHttpClient client) {
    this.okHttpClient = client;
  }

  /**
   * Configures a new HTTP client instance.
   *
   * @return the HTTP client
   */
  private OkHttpClient configureHttpClient() {
    final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    addCookieJar(builder);

    builder.connectTimeout(60, TimeUnit.SECONDS);
    builder.writeTimeout(60, TimeUnit.SECONDS);
    builder.readTimeout(90, TimeUnit.SECONDS);

    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledCipherSuites().build();
    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));

    setupTLSProtocol(builder);

    return builder.build();
  }

  /**
   * Adds the cookie jar.
   *
   * @param builder the builder
   */
  private void addCookieJar(final OkHttpClient.Builder builder) {
    final CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    builder.cookieJar(new ServiceCookieJar(cookieManager));
  }

  /**
   * Modifies the specified {@link OkHttpClient} instance to not verify SSL certificates.
   * @param client the {@link OkHttpClient} instance to disable SSL on
   */
  private OkHttpClient disableSslVerification(OkHttpClient client) {
    SSLContext trustAllSslContext;
    try {
      trustAllSslContext = SSLContext.getInstance("SSL");
      trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }

    SSLSocketFactory trustAllSslSocketFactory = new FilteredSSLSocketFactory(trustAllSslContext.getSocketFactory());

    OkHttpClient.Builder builder = client.newBuilder();
    builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0]);
    builder.hostnameVerifier(new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    });

    return builder.build();
  }

  /**
   * Sets a proxy for the specified {@link OkHttpClient} instance and returns
   * a new instance with the proxy configured as requested.
   *
   * @param client the {@link OkHttpClient} instance to set the proxy on
   * @param proxy the {@link Proxy}
   * @return the new {@link OkHttpClient} instance with the proxy configured
   */
  private OkHttpClient setProxy(OkHttpClient client, Proxy proxy) {
    OkHttpClient.Builder builder = client.newBuilder().proxy(proxy);
    return builder.build();
  }

  /**
   * Sets a proxy authenticator for the specified {@link OkHttpClient} instance and returns
   * a new instance with the authentication configured as requested.
   *
   * @param client the {@link OkHttpClient} instance to set the proxy authenticator on
   * @param proxyAuthenticator the {@link Authenticator}
   * @return the new {@link OkHttpClient} instance with the authenticator configured
   */
  private OkHttpClient setProxyAuthenticator(OkHttpClient client, Authenticator proxyAuthenticator) {
    OkHttpClient.Builder builder = client.newBuilder().proxyAuthenticator(proxyAuthenticator);
    return builder.build();
  }

  /**
   * Sets the logging level for the specified {@link OkHttpClient} instance and returns
   * a new instance with the logging configured as requested.
   *
   * @param client the {@link OkHttpClient} instance to set the proxy authenticator on
   * @param loggingLevel the {@link LoggingLevel}
   * @return the new {@link OkHttpClient} instance with the logging configured
   */
  private OkHttpClient setLoggingLevel(OkHttpClient client, LoggingLevel loggingLevel) {
    // First check to see if the client already has the http logging interceptor registered.
    // If not, then we'll register a new instance of one.
    HttpLogger loggingInterceptor = null;
    for (Interceptor i : client.networkInterceptors()) {
      if (i instanceof HttpLogger) {
        loggingInterceptor = (HttpLogger) i;
      }
    }

    OkHttpClient updatedClient = client;
    if (loggingInterceptor == null) {
      loggingInterceptor = new HttpLogger();
      OkHttpClient.Builder builder = client.newBuilder().addNetworkInterceptor(loggingInterceptor);
      updatedClient = builder.build();
    }

    switch (loggingLevel) {
      case BODY:
        loggingInterceptor.setLevel(HttpLogger.Level.BODY);
        break;
      case HEADERS:
        loggingInterceptor.setLevel(HttpLogger.Level.HEADERS);
        break;
      case BASIC:
        loggingInterceptor.setLevel(HttpLogger.Level.BASIC);
        break;
      default:
        loggingInterceptor.setLevel(HttpLogger.Level.NONE);
    }

    return updatedClient;
  }

  /**
   * Specifically enable all TLS protocols. See: https://github.com/watson-developer-cloud/java-sdk/issues/610
   *
   * @param builder the {@link OkHttpClient} builder.
   */
  public static void setupTLSProtocol(final OkHttpClient.Builder builder) {
    try {
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init((KeyStore) null);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

      if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
        throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
      }

      X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

      // On IBM JDKs this gets only TLSv1
      SSLContext sslContext = SSLContext.getInstance("TLS");

      sslContext.init(null, new TrustManager[] { trustManager }, null);
      SSLSocketFactory sslSocketFactory = new FilteredSSLSocketFactory(sslContext.getSocketFactory());
      builder.sslSocketFactory(sslSocketFactory, trustManager);

    } catch (NoSuchAlgorithmException e) {
      LOG.log(Level.SEVERE, "The cryptographic algorithm requested is not available in the environment.", e);
    } catch (KeyStoreException e) {
      LOG.log(Level.SEVERE, "Error using the keystore.", e);
    } catch (KeyManagementException e) {
      LOG.log(Level.SEVERE, "Error initializing the SSL Context.", e);
    }
  }

  /**
   * Sets a new list of interceptors for the specified {@link OkHttpClient} instance by removing the specified
   * interceptor and returns a new instance with the interceptors configured as requested.
   *
   * @param client the {@link OkHttpClient} instance to remove the interceptors from
   * @param interceptorToRemove the class name of the interceptor to remove
   * @return the new {@link OkHttpClient} instance with the new list of interceptors
   */
  private OkHttpClient reconfigureClientInterceptors(OkHttpClient client, String interceptorToRemove) {
    OkHttpClient.Builder builder = client.newBuilder();

    if (!builder.interceptors().isEmpty()) {
      for (Iterator<Interceptor> iter = builder.interceptors().iterator(); iter.hasNext(); ) {
        Interceptor element = iter.next();
        if (interceptorToRemove.equals(element.getClass().getSimpleName())) {
          iter.remove();
        }
      }
    }

    return builder.build();
  }

  /**
   * Sets a new list of interceptors for the specified {@link OkHttpClient} instance by removing any interceptors
   * that implement "interfaceToRemove".
   *
   * @param client the {@link OkHttpClient} instance to remove the interceptors from
   * @param interfaceToRemove the specific interface for which interceptor instances should be removed
   * @return the new {@link OkHttpClient} instance with the updated list of interceptors
   */
  private OkHttpClient reconfigureClientInterceptors(OkHttpClient client,
      Class<? extends Interceptor> interfaceToRemove) {
    OkHttpClient.Builder builder = client.newBuilder();

    if (!builder.interceptors().isEmpty()) {
      for (Iterator<Interceptor> iter = builder.interceptors().iterator(); iter.hasNext(); ) {
        Interceptor element = iter.next();
        if (interfaceToRemove.isAssignableFrom(element.getClass())) {
          iter.remove();
        }
      }
    }

    return builder.build();
  }

  /**
   * Creates a new {@link OkHttpClient} instance with a new {@link ServiceCookieJar}
   * and a default configuration.
   *
   * @return the new {@link OkHttpClient} instance
   */
  public OkHttpClient createHttpClient() {
    Builder builder = okHttpClient.newBuilder();
    addCookieJar(builder);
    return builder.build();
  }

  /**
   * Configures the current {@link OkHttpClient} instance based on the passed-in options, replaces
   * the current instance with the newly-configured instance and returns the new instance.
   *
   * @param options the {@link HttpConfigOptions} object for modifying the client instance
   * @return the new client instance
   */
  public OkHttpClient configureClient(HttpConfigOptions options) {
    this.okHttpClient = configureClient(this.okHttpClient, options);
    return this.okHttpClient;
  }

  /**
   * Configures the specified {@link OkHttpClient} instance based on the passed-in options,
   * and returns a new instance with the requested options applied.
   *
   * @param client the {@link OkHttpClient} instance to configure
   * @param options the {@link HttpConfigOptions} instance for modifying the client
   * @return a new {@link OkHttpClient} instance with the specified options applied
   */
  public OkHttpClient configureClient(OkHttpClient client, HttpConfigOptions options) {
    if (options != null) {
      if (options.shouldDisableSslVerification()) {
        client = disableSslVerification(client);
      }
      if (options.getProxy() != null) {
        client = setProxy(client, options.getProxy());
      }
      if (options.getProxyAuthenticator() != null) {
        client = setProxyAuthenticator(client, options.getProxyAuthenticator());
      }
      if (options.getLoggingLevel() != null) {
        client = setLoggingLevel(client, options.getLoggingLevel());
      }

      // Configure the retry interceptor.
      Boolean enableRetries = options.getRetries();
      if (enableRetries != null) {
        client = reconfigureClientInterceptors(client, IRetryInterceptor.class);
        if (enableRetries.booleanValue()) {
          IRetryInterceptor retryInterceptor =
              retryStrategy.createRetryInterceptor(options.getMaxRetries(), options.getMaxRetryInterval(),
                  options.getAuthenticator());
          if (retryInterceptor != null) {
            client = client.newBuilder().addInterceptor(retryInterceptor).build();
          } else {
            LOG.log(Level.WARNING,
                "The retry interceptor factory returned a null IRetryInterceptor instance. Retries are disabled.");
          }
        }
      }

      // Configure the GZIP interceptor.
      Boolean enableGzip = options.getGzipCompression();
      if (enableGzip != null) {
        client = reconfigureClientInterceptors(client, "GzipRequestInterceptor");
        if (enableGzip.booleanValue()) {
          client = client.newBuilder()
                  .addInterceptor(new GzipRequestInterceptor())
                  .build();
        }
      }
    }
    return client;
  }
}
