//
// Copyright 2024 IBM Corporation.
// SPDX-License-Identifier: Apache2.0
//

package com.ibm.cloud.sdk.core.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.util.LoggingUtils;

import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.GzipSource;

/**
 * An okhttp Interceptor that performs custom logging of HTTP request and response messages.
 */
public class HttpLogger implements Interceptor {
  private static final Logger LOG = Logger.getLogger(HttpLogger.class.getName());

  enum Level {
    NONE,
    BASIC,
    HEADERS,
    BODY
  }

  private Level level = Level.NONE;

  public void setLevel(Level l) {
    this.level = l;
  }
  public Level getLevel() {
    return this.level;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (this.getLevel() == Level.NONE) {
      // If HTTP logging is set to NONE, then just immediately process the request
      // and be done with it.
      return chain.proceed(request);
    }

    boolean logBody = this.getLevel() == Level.BODY;
    boolean logHeaders = logBody || this.getLevel() == Level.HEADERS;

    RequestBody requestBody = request.body();

    Connection c = chain.connection();
    Protocol p = c != null ? c.protocol() : null;
    String protocol = p != null ? " " + p.toString() : "";

    // Use a StringBuilder to build the log output for the request message.
    StringBuilder msg = new StringBuilder();
    String requestBodyMsg = "";
    if (!logHeaders && requestBody != null) {
      requestBodyMsg = String.format(" (%d-byte body)", requestBody.contentLength());
    }
    msg.append(String.format("--> HTTP Request:\n%s %s%s%s\n", request.method(),
        request.url().toString(), protocol, requestBodyMsg));

    if (logHeaders) {
      logHeaders(request.headers(), msg);

      if (!logBody || requestBody == null) {
        msg.append(String.format("--> END %s", request.method()));
      } else if (bodyHasUnknownEncoding(request.headers())) {
        msg.append(String.format("--> END %s (encoded body omitted)", request.method()));
      } else if (requestBody.isDuplex()) {
        msg.append(String.format("--> END %s (duplex request body omitted)", request.method()));
      } else if (requestBody.isOneShot()) {
        msg.append(String.format("--> END %s (one-shot body omitted)", request.method()));
      } else {
        okio.Buffer buffer = new okio.Buffer();
        requestBody.writeTo(buffer);
        Long gzippedLength = null;
        String contentEncoding = request.headers().get("Content-Encoding");
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
          gzippedLength = Long.valueOf(buffer.size());
          GzipSource gzippedBody = null;
          try {
            gzippedBody = new GzipSource(buffer.clone());
            buffer = new okio.Buffer();
            buffer.writeAll(gzippedBody);
          } catch (Throwable t) {
            // Absorb any exception caught here.
          }
        }

        okhttp3.MediaType contentType = requestBody.contentType();
        Charset charset = null;
        if (contentType != null) {
          charset = contentType.charset(StandardCharsets.UTF_8);
        }
        if (charset == null) {
          charset = StandardCharsets.UTF_8;
        }

        long buflen = buffer.size();
        try {
          String s = buffer.readString(charset);
          msg.append("\n").append(s).append("\n");
          if (gzippedLength != null) {
            msg.append(String.format("--> END %s (%d-byte, %d-gzipped-byte body)",
                request.method(), buflen, gzippedLength.intValue()));
          } else {
            msg.append(String.format("--> END %s (%d-byte body)", request.method(), buflen));
          }
       } catch (Throwable t) {
          msg.append(String.format("--> END %s (binary %d-byte body omitted)", request.method(), buflen));
        }
      }
    }

    // Dump the request message..
    LOG.fine(LoggingUtils.redactSecrets(msg.toString()));

    long startNS = System.nanoTime();
    Response response;
    try {
      response = chain.proceed(request);
    } catch (Throwable t) {
      LOG.log(java.util.logging.Level.SEVERE, "<-- HTTP FAILED: ", t);
      throw t;
    }

    // Use a new StringBuilder to build the log output for the response message.
    msg = new StringBuilder();

    long tookMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNS);

    ResponseBody responseBody = response != null ? response.body() : null;
    long contentLength = responseBody != null ? responseBody.contentLength() : -1L;
    String bodySize = contentLength >= 0 ? String.format("%d-byte", contentLength) : "unknown-length";
    msg.append(String.format("<-- HTTP Response:\n%d", response.code()));
    if (StringUtils.isNotEmpty(response.message())) {
      msg.append(" ").append(response.message());
    }

    msg.append(" ").append(response.request().url().toString());

    msg.append(String.format(" (%dms", tookMS));
    if (!logHeaders) {
      msg.append(String.format(", %s body", bodySize));
    }
    msg.append(")\n");

    if (logHeaders) {
      logHeaders(response.headers(), msg);

      if (!logBody || responseBody == null) {
        msg.append("<-- END HTTP");
      } else if (bodyHasUnknownEncoding(response.headers())) {
        msg.append("--> END HTTP (encoded body omitted)");
      } else {
        okio.BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE);
        okio.Buffer buffer = source.buffer();
        Long gzippedLength = null;
        String contentEncoding = response.headers().get("Content-Encoding");
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
          gzippedLength = Long.valueOf(buffer.size());
          GzipSource gzippedBody = null;
          try {
            gzippedBody = new GzipSource(buffer.clone());
            buffer = new okio.Buffer();
            buffer.writeAll(gzippedBody);
          } catch (Throwable t) {
            // Absorb any exception caught here.
          }
        }

        okhttp3.MediaType contentType = responseBody.contentType();
        Charset charset = null;
        if (contentType != null) {
          charset = contentType.charset(StandardCharsets.UTF_8);
        }
        if (charset != null) {
          charset = StandardCharsets.UTF_8;
        }

        try {
          if (contentLength > 0) {
            String s = buffer.clone().readString(charset);
            msg.append("\n").append(s).append("\n");

            if (gzippedLength != null) {
              msg.append(String.format("<-- END HTTP (%d-byte, %d-gzipped-byte body)",
                  buffer.size(), gzippedLength.intValue()));
            } else {
              msg.append(String.format("<-- END HTTP (%d-byte body)", buffer.size()));
            }
          }
        } catch (Throwable t) {
          if (gzippedLength != null) {
            msg.append(String.format("<-- END HTTP (binary %d-byte, %d-gzipped-byte body omitted)",
                buffer.size(), gzippedLength.intValue()));
          } else {
            msg.append(String.format("<-- END HTTP (binary %d-byte body omitted)", buffer.size()));
          }
        }
      }
    }

    // Dump the response message.
    LOG.fine(LoggingUtils.redactSecrets(msg.toString()));

    return response;
  }

  /**
   * "Logs" the headers contained in "headers" by adding messages to
   * the StringBuilder "sb".
   * @param headers the headers to be logged
   * @param sb the StringBuilder to which messages will be added
   */
  private void logHeaders(okhttp3.Headers headers, StringBuilder sb) {
    for (int i = 0; i < headers.size(); i++) {
      sb.append(String.format("%s: %s\n", headers.name(i), headers.value(i)));
    }
  }

  /**
   * Returns true iff "headers" has an unknown Content-Encoding header value.
   * In this context "unknown" implies that the Content-Encoding header was
   * set to something other than "identity" or "gip" (these are the only "known" encoding values).
   * @param headers the headers to check
   * @return true if an unknown encoding value was found
   */
  private boolean bodyHasUnknownEncoding(okhttp3.Headers headers) {
    String contentEncoding = headers.get("Content-Encoding");
    if (StringUtils.isEmpty(contentEncoding)) {
      return false;
    }

    return !"identity".equalsIgnoreCase(contentEncoding) && !"gzip".equalsIgnoreCase(contentEncoding);
  }
}
