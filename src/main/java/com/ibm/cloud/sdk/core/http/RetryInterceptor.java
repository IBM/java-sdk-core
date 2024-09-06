/**
 * (C) Copyright IBM Corp. 2021, 2024.
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

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.util.DateUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is an okhttp Interceptor implementation that will try to automatically retry
 * failed requests, based on the type of failure that occurred.
 * This class is configured with the following:
 * <ul>
 * <li>the maximum number of retries to attempt for a failed request
 * <li>the maximum retry interval (in seconds) to wait between retry attempts
 * <li>the {@link Authenticator} instance to use to authenticate each retry attempt
 * </ul>
 */
public class RetryInterceptor implements IRetryInterceptor {
  private static final Logger LOG = Logger.getLogger(RetryInterceptor.class.getName());

  // The default "starting" retry interval in milliseconds.
  private static final int DEFAULT_RETRY_INTERVAL = 1000;

  private Authenticator authenticator;
  private int maxRetries;
  private int maxRetryInterval;

  public class RetryContext {
    private int retryCount;

    private RetryContext() {
    }

    private int getRetryCount() {
      return this.retryCount;
    }

    private boolean incCountAndCheck() {
      this.retryCount++;
      return this.retryCount < maxRetries;
    }
  }

  // Hide the default ctor to force the use of the non-default ctor below.
  protected RetryInterceptor() { }

  /**
   * This ctor configures the RetryInterceptor instance with the max retries,
   * retry interval and an authenticator.
   * @param maxRetries the maximum number of retries to attempt for a failed request
   * @param maxRetryInterval the maximum retry interval (in seconds) to wait between retry attempts
   * @param authenticator the {@link Authenticator} instance to use to authenticate retried requests
   */
  public RetryInterceptor(int maxRetries, int maxRetryInterval, Authenticator authenticator) {
    this.authenticator = authenticator;
    this.maxRetries = maxRetries;
    // Convert the interval from seconds to milliseconds.
    this.maxRetryInterval = maxRetryInterval * 1000;
  }

  /**
   * The "intercept()" method is the primary method of the interceptor.
   * The chain of interceptors registered for a particular okhttp Client instance
   * is in the form of an ordered list.  When a request is invoked, each interceptor's
   * "intercept" method is invoked and is passed the interceptor chain.
   * The interceptor can inspect the request to determine how to proceed, and will invoke the interceptor
   * chain's "proceed()" method to call the next interceptor in the chain.
   * When the last interceptor invokes the chain's "proceed()" method, the request is sent over the wire
   * and the response is returned via the return value of the proceed() method.
   * The interceptor can then inspect the response and determine how to proceed.
   * Ultimately the response is returned from this intercept() method and is ultimately propagated
   * back through the chain's "proceed()" methods.
   */
  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    // Make the first request.
    Request request = chain.request();
    Response response = chain.proceed(request);

    while (shouldRetry(response, request)) {
      int interval = getInterval(response, request);

      try {
        LOG.log(Level.FINE, "Will retry after {0} ms", interval);
        Thread.sleep(interval);
      } catch (InterruptedException e) {
        LOG.log(Level.FINE, "Thread was interrupted; the invocation has likely been cancelled.");
      }

      Request.Builder builder = request.newBuilder();

      // If this is the first retry, create the context and attach it to the requests.
      if (request.tag(RetryContext.class) == null) {
        builder.tag(RetryContext.class, new RetryContext());
      }

      // If we have a valid authenticator, then authenticate the request.
      // This is mostly here for backward compatibility.
      if (authenticator != null) {
        authenticator.authenticate(builder);
      }

      response.close();
      request = builder.build();
      response = chain.proceed(request);
    }

    return response;
  }

  /**
   * Determine the retry interval to wait before attempting the next retry.
   * @param response the response from the previously attempted request
   * @param request the previously attempted request
   * @return the retry interval in milliseconds
   */
  protected int getInterval(Response response, Request request) {
    Integer interval = null;

    String headerVal = response.header("Retry-After");

    if (StringUtils.isNotEmpty(headerVal)) {
      LOG.log(Level.FINE, "Detected Retry-After header in response: {0}", headerVal);
      int responseInterval = 0;
      // First, try to parse as an integer (number of seconds to wait).
      try {
        responseInterval = Integer.parseInt(headerVal, 10) * 1000;
      } catch (NumberFormatException e) {
        // If we cannot parse it as an integer, let's try to do it as an HTTP date value.
        try {
          Date retryTime = DateUtils.parseAsDateTime(headerVal);
          responseInterval = (int) Instant.now().until(retryTime.toInstant(), ChronoUnit.MILLIS);
        } catch (DateTimeException dte) {
          LOG.log(Level.WARNING,
              "Response included a non-numeric and non-HTTP Date value for Retry-After: {0}", headerVal);
        }
      }
      // Just in case it's a negative number.
      if (responseInterval > 0) {
        interval = Integer.valueOf(responseInterval);
      }
    }

    // We couldn't infer the interval from the response, so let's calculate it.
    if (interval == null) {
      RetryContext context = request.tag(RetryContext.class);
      if (context != null) {
        interval = calculateBackoff(context.getRetryCount());
      } else {
        // There is no RetryContext tag in the request, which means this is the first retry.
        interval = calculateBackoff(0);
      }
    }

    return interval;
  }

  /**
   * Determine whether or not to attempt a retry of the specified request.
   * @param response the response obtained from the previously-attempted request
   * @param request the previously-attempted request
   * @return true if the specified request should be retried, false otherwise
   */
  protected boolean shouldRetry(Response response, Request request) {
    LOG.log(Level.FINE, "Considering retry attempt; status_code={0}, method={1}, url={2}",
        new Object[] { response.code(), request.method(), request.url().toString()});

    // First check the response.
    if (response.code() == 429 || (response.code() >= 500 && response.code() <= 599 && response.code() != 501)) {
      // Now check if we exhausted the max number of retries or not.
      RetryContext context = request.tag(RetryContext.class);
      if (context != null && !context.incCountAndCheck()) {
        LOG.log(Level.FINE, "No retry, maximum number of retries reached");
        return false;
      }

      LOG.log(Level.FINE, "Retry will be attempted");
      return true;
    }

    LOG.log(Level.FINE, "No retry, response code not eligible");
    return false;
  }

  /**
   * Compute the "backoff" time (retry interval) in milleseconds based on the retry count.
   * This calculation is based on the go-retryablehttp package's "DefaultBackoff()" function.
   * @param retryCount the retry count for which we need to compute the backoff time
   * @return the retry interval to use for retry number "retryCount"
   */
  protected int calculateBackoff(int retryCount) {
    // Exponential interval calculation based on the number of retries.
    double newInterval = (Math.pow(2, Double.valueOf(retryCount))) * RetryInterceptor.DEFAULT_RETRY_INTERVAL;
    if (newInterval > this.maxRetryInterval) {
      return this.maxRetryInterval;
    }

    return (int) newInterval;
  }
}
