package com.ibm.cloud.sdk.core.http.ratelimit;

/**
 * Carries per request state for {@link RateLimitInterceptor}.
 */
public class RateLimitContext {
    private int remainingRetries;

    public RateLimitContext(int maxRetries) {

        this.remainingRetries = maxRetries;
    }

    public boolean decrementAndCheck() {
        remainingRetries--;
        return remainingRetries > 0;
    }

    @Override
    public String toString() {
        return "RateLimitContext{"
                + "remainingRetries="
                + remainingRetries + '}';
    }
}
