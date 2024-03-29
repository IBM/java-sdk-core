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

package com.ibm.cloud.sdk.core.http;

import java.net.Proxy;

import com.ibm.cloud.sdk.core.http.ratelimit.RateLimitConstants;

import okhttp3.Authenticator;

/**
 * Options class for configuring the HTTP client.
 */
public class HttpConfigOptions {

  /**
   * Levels of information to log when making HTTP requests, from least (NONE) to most (BODY).
   */
  public enum LoggingLevel {
    NONE,
    BASIC,
    HEADERS,
    BODY,
  }

  private boolean disableSslVerification;
  private Boolean enableGzipCompression;
  private Boolean enableRetries;
  private int maxRetries;
  private int maxRetryInterval;
  private Proxy proxy;
  private Authenticator proxyAuthenticator;
  private LoggingLevel loggingLevel;

  // Ratelimiting properties
  private com.ibm.cloud.sdk.core.security.Authenticator authenticator;

  public boolean shouldDisableSslVerification() {
    return this.disableSslVerification;
  }

  public Boolean getGzipCompression() {
    return this.enableGzipCompression;
  }

  public Boolean getRetries() {
    return this.enableRetries;
  }

  public int getMaxRetries() {
    return this.maxRetries;
  }

  public int getMaxRetryInterval() {
    return this.maxRetryInterval;
  }

  public Proxy getProxy() {
    return this.proxy;
  }

  public Authenticator getProxyAuthenticator() {
    return this.proxyAuthenticator;
  }

  public LoggingLevel getLoggingLevel() {
    return this.loggingLevel;
  }

  public com.ibm.cloud.sdk.core.security.Authenticator getAuthenticator() {
    return authenticator;
  }

  /**
   * @return the default retry interval
   *
   * @deprecated As of 9.13.0, use HttpConfigOptions.getMaxRetryInterval() instead.
   */
  @Deprecated
  public int getDefaultRetryInterval() {
    return getMaxRetryInterval();
  }

  public static class Builder {
    private boolean disableSslVerification;
    private Boolean enableGzipCompression;
    private Boolean enableRetries;
    private int maxRetries;
    private int maxRetryInterval;
    private Proxy proxy;
    private Authenticator proxyAuthenticator;
    private LoggingLevel loggingLevel;

    // Ratelimiting properties
    private com.ibm.cloud.sdk.core.security.Authenticator authenticator;

    public HttpConfigOptions build() {
      return new HttpConfigOptions(this);
    }

    /**
     * Sets flag to disable any SSL certificate verification during HTTP requests. This should ONLY be used if truly
     * intended, as it's unsafe otherwise.
     *
     * @param disableSslVerification whether to disable SSL verification or not
     * @return the builder
     */
    public Builder disableSslVerification(boolean disableSslVerification) {
      this.disableSslVerification = disableSslVerification;
      return this;
    }

    /**
     * Sets flag to enable gzip compression of request bodies during HTTP requests. This should ONLY be used if truly
     * intended, as many webservers can't handle this.
     *
     * @param enableGzipCompression whether to disable SSL verification or not
     * @return the builder
     */
    public Builder enableGzipCompression(Boolean enableGzipCompression) {
      this.enableGzipCompression = enableGzipCompression;
      return this;
    }

    /**
     * Sets retry on rate limiting policy (429). See  {@link RateLimitConstants} for defaults to use
     *
     * @param authenticator to use for retries, the {@link Authenticator} used by the client
     * @param defaultInterval if not specified in the response, how long to wait until the next attempt
     * @param maxRetries the maximum amount of retries for an request
     * @return the builder
     *
     * @deprecated As of 9.13.0, use the HttpConfigOptions.enableRetries() instead.
     */
    @Deprecated
    public Builder enableRateLimitRetry(com.ibm.cloud.sdk.core.security.Authenticator authenticator
            , int defaultInterval, int maxRetries) {
      return enableRetries(authenticator, maxRetries, defaultInterval);
    }

    /**
     * Sets retry specific parameters to enable it.
     *
     * @param authenticator to use for retries, the {@link Authenticator} used by the client
     * @param maxRetries the maximum amount of retries for a request
     * @param maxRetryInterval if not specified in the response, how long to wait until the next attempt
     * @return the builder
     */
    public Builder enableRetries(com.ibm.cloud.sdk.core.security.Authenticator authenticator, int maxRetries,
        int maxRetryInterval) {
      this.enableRetries = true;
      this.authenticator = authenticator;
      this.maxRetries = maxRetries;
      this.maxRetryInterval = maxRetryInterval;
      return this;
    }

    /**
     * Sets the retry specific parameter to zero to disable the retry functionality.
     *
     * @return the builder
     */
    public Builder disableRetries() {
      this.enableRetries = false;
      this.maxRetries = 0;
      this.maxRetryInterval = 0;
      return this;
    }

    /**
     * Sets HTTP proxy to be used by connections with the current client.
     *
     * @param proxy the desired {@link Proxy}
     * @return the builder
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Sets HTTP proxy authentication to be used by connections with the current client.
     *
     * @param proxyAuthenticator the desired {@link Authenticator}
     * @return the builder
     */
    public Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
      this.proxyAuthenticator = proxyAuthenticator;
      return this;
    }

    /**
     * Sets HTTP logging level to be used by the current client.
     *
     * @param loggingLevel the {@link LoggingLevel} specifying how much information should be logged
     * @return the builder
     */
    public Builder loggingLevel(LoggingLevel loggingLevel) {
      this.loggingLevel = loggingLevel;
      return this;
    }
  }

  private HttpConfigOptions(Builder builder) {
    this.disableSslVerification = builder.disableSslVerification;
    this.enableGzipCompression = builder.enableGzipCompression;
    this.enableRetries = builder.enableRetries;
    this.maxRetries = builder.maxRetries;
    this.maxRetryInterval = builder.maxRetryInterval;
    this.proxy = builder.proxy;
    this.proxyAuthenticator = builder.proxyAuthenticator;
    this.loggingLevel = builder.loggingLevel;
    // rate limiting related
    this.authenticator = builder.authenticator;
  }
}
