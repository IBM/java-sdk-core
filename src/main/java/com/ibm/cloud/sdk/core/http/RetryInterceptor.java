package com.ibm.cloud.sdk.core.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Logger;

import com.ibm.cloud.sdk.core.util.DateUtils;

/**
 * This interceptor checks the responses and retries the request if it's possible.
 */
public class RetryInterceptor implements Interceptor {

  private static final Logger LOG = Logger.getLogger(RetryInterceptor.class.getName());

  // The default "starting" retry interval in milliseconds.
  private static final int DEFAULT_RETRY_INTERVAL = 1000;

  private int maxRetries;
  private int maxRetryInterval;

  private class RetryContext {
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

  public RetryInterceptor(int maxRetries, int maxRetryInterval) {
    this.maxRetries = maxRetries;
    // Convert the interval from seconds to milliseconds.
    this.maxRetryInterval = maxRetryInterval * 1000;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    // Make the first request.
    Request request = chain.request();
    Response response = chain.proceed(request);

    while (shouldRetry(response, request)) {
      int interval = getInterval(response, request);

      try {
        LOG.fine("Will retry after: " + interval + "ms");
        Thread.sleep(interval);
      } catch (InterruptedException e) {
        LOG.fine("Thread was interrupted, likely the call has been cancelled.");
      }

      Request.Builder builder = request.newBuilder();

      // If this is the first retry, create the context and attach it to the requests.
      if (request.tag(RetryContext.class) == null) {
        builder.tag(RetryContext.class, new RetryContext());
      }

      response.close();
      request = builder.build();
      response = chain.proceed(request);
    }

    return response;
  }

  // Get the time we should wait before fire the next request in milliseconds.
  private int getInterval(Response response, Request request) {
    Integer interval = null;

    String headerVal = response.header("Retry-After");

    if (headerVal != null && !headerVal.equals("")) {
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
          LOG.info("Response included a non numberic and non HTTP Date value for Retry-After: " + headerVal);
        }
      }
      // Just in case it's a negative number.
      if (responseInterval > 0) {
        interval = responseInterval;
      }
    }

    // So we cannot use interval from the response so let's calculate it.
    if (interval == null) {
      RetryContext context = request.tag(RetryContext.class);
      if (context != null) {
        interval = calculateBackoff(context.getRetryCount());
      } else {
        // There is RetryContext tag in the request, which means this is the first retry.
        interval = calculateBackoff(0);
      }
    }

    return interval;
  }

  // Check the response and the retry context then decide should we have make
  // another request.
  private boolean shouldRetry(Response response, Request request) {
    // First check the response.
    if (response.code() == 429 || (response.code() >= 500 && response.code() != 501)) {
      // Now check if we exhausted the max number of retries or not.
      RetryContext context = request.tag(RetryContext.class);
      if (context != null && !context.incCountAndCheck()) {
        return false;
      }

      return true;
    }

    return false;
  }

  // Calculate the back off time in milliseconds based on the retry count.
  // This calculation is based on what the go-retryablehttp package does in its
  // DefaultBackoff function.
  private int calculateBackoff(int retryCount) {
    // Exponential interval calculation based on the number of retries.
    double newInterval = (Math.pow(2, Double.valueOf(retryCount))) * RetryInterceptor.DEFAULT_RETRY_INTERVAL;
    if (newInterval > this.maxRetryInterval) {
      return this.maxRetryInterval;
    }

    return (int) newInterval;
  }
}
