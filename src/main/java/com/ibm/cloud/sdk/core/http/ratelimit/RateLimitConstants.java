package com.ibm.cloud.sdk.core.http.ratelimit;


/**
 * This class encapsulate constants that can be passed as defaults to {@link RateLimitInterceptor}.
 *
 * @deprecated As of 9.13.0, use the RetryInterceptor instead.
 */
@Deprecated
public interface RateLimitConstants {
    /**
     * Time to wait before retrying in absence of information from server.
     */
    int DEFAULT_INTERVAL = 5000;
    /**
     * Maximum amount of times a request will be retried.
     */
    int MAX_RETRIES = Integer.MAX_VALUE;
}
