package com.ibm.cloud.sdk.core.http;

import okhttp3.Authenticator;

import java.net.Proxy;

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
  private Proxy proxy;
  private Authenticator proxyAuthenticator;
  private LoggingLevel loggingLevel;

  public boolean shouldDisableSslVerification() {
    return this.disableSslVerification;
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

  public static class Builder {
    private boolean disableSslVerification;
    private Proxy proxy;
    private Authenticator proxyAuthenticator;
    private LoggingLevel loggingLevel;

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
    this.proxy = builder.proxy;
    this.proxyAuthenticator = builder.proxyAuthenticator;
    this.loggingLevel = builder.loggingLevel;
  }
}
