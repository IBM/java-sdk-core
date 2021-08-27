package com.ibm.cloud.sdk.core.http.ratelimit;

import com.ibm.cloud.sdk.core.security.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Provides means to retry requests on RateLimiting (429).
 *
 * @deprecated As of 9.13.0, use the RetryInterceptor instead.
 */
@Deprecated
public class RateLimitInterceptor implements Interceptor {

    private static final Logger LOG = Logger.getLogger(RateLimitInterceptor.class.getName());

    private int defaultInterval;
    private int maxRetries;
    private Authenticator authenticator;

    public RateLimitInterceptor(Authenticator authenticator, int defaultInterval, int maxRetries) {
        this.defaultInterval = defaultInterval;
        this.maxRetries = maxRetries;
        this.authenticator = authenticator;
    }


    /**
     * Checks response and retries with delay in case of a 429, time between attempts is taken from
     * response, if no valid information is on the response, the default is used.
     */
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        // 429 indicates a rate limit error
        while (shouldRetry(response, request)) {
            int interval = getInterval(response);

            // wait & retry
            // A dispatcher based model may be more efficient.
            try {
                LOG.info("Will retry after: " + interval + "ms");
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                LOG.fine("Thread was interrupted, likely call cancelled");
            }

            // At this point, time has passed, so we want to ensure auth is up-to date,
            // as well as ensure we embed a context to carry state forward
            Request.Builder builder = request.newBuilder();

            // if this is the first round, no tag exists yet
            if (request.tag(RateLimitContext.class) == null) {
                builder = builder.tag(RateLimitContext.class, new RateLimitContext(maxRetries));
            }
            if (authenticator != null) {
                authenticator.authenticate(builder);
            }
            response.close();
            request = builder.build();
            response = chain.proceed(request);
        }

        return response;
    }

    private int getInterval(Response response) {
        // if the server didn't provide details, we'll still wait default interval
        int interval = defaultInterval;

        // Both headers can be used concurrently, but need to be consistent.
        // draft allows for more fine-grained control, we just cover the basics for now
        // https://tools.ietf.org/id/draft-polli-ratelimit-headers-00.html
        String headerVal = response.header("RateLimit-Reset");

        // RFC 7231, section 7.1.3: Retry-After
        if (headerVal == null) {
            headerVal = response.header("Retry-After");
        }

        // According to spec, this will be a integer, if it's not, we're falling back to default
        if (headerVal != null) {
            try {
                int responseInterval = Integer.parseInt(headerVal, 10) * 1000;
                // just in case it's a negative number
                if (responseInterval > 0) {
                    interval = responseInterval;
                }
            } catch (NumberFormatException e) {
                LOG.info("Response included a non-numeric value for Retry-After/RateLimit-Reset");
            }
        }
        return interval;
    }

    private boolean shouldRetry(Response response, Request request) {
        // if we got 429, and we didn't exhaust maxRetries, we should retry
        if (!response.isSuccessful() && response.code() == 429) {
            // the first attempt won't have a context yet, so we need to check
            RateLimitContext context = request.tag(RateLimitContext.class);
            if (context != null && !context.decrementAndCheck()) {
                LOG.info("Retries exhausted for RateLimit, giving up");
                return false;
            }
            if (context != null) {
                LOG.fine(context.toString());
            }
            return true;
        }
        return false;
    }
}
