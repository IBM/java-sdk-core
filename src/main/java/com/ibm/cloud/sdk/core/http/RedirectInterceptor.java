/**
 * (C) Copyright IBM Corp. 2023.
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
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;

/**
 * This class is an okhttp Interceptor implementation that will automatically
 * handle redirects, while also allowing "safe" headers to be included in
 * cross-site redirect requests.
 */
public class RedirectInterceptor implements Interceptor {
    private static final Logger LOG = Logger.getLogger(RedirectInterceptor.class.getName());

    // Max # of redirects supported by this interceptor.
    // Note that okhttp supports up to 20 "follow-ups" (redirects + retries).
    // We'll go with a max of 10 redirects to align with the other cores.
    private static final int MAX_REDIRECTS = 10;

    // This is considered a "safe domain" (i.e. if both hosts are located
    // within the IBM Cloud domain, then it's considered a "safe" redirect).
    private static final String SAFE_DOMAIN = ".cloud.ibm.com";

    // These are the HTTP headers that are stripped out of a request considered
    // to be an unsafe redirect.
    private static List<String> safeHeaders = Arrays.asList("Authorization", "WWW-Authenticate", "Cookie", "Cookie2");

    /**
     * Default ctor.
     */
    public RedirectInterceptor() {
    }

    /**
     * This is the primary method invoked by OkHttp when processing interceptors.
     * This interceptor will handle automatic redirects up to a maximum of MAX_REDIRECTS.
     */
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        // Grab our original request from the chain.
        Request request = chain.request();

        // Invoke the first request, then react to the response.
        Response response = chain.proceed(request);

        // Keep processing responses while they indicate a redirect.
        int redirectCount = 0;
        while (isRedirectStatusCode(response.code())) {
            LOG.fine(String.format("Received redirect response code (%d) for request %s %s",
                    response.code(), request.method(), request.url().toString()));

            // Check to see if we've exhausted the max redirects for this request.
            redirectCount++;
            if (redirectCount > MAX_REDIRECTS) {
                LOG.fine(String.format(
                        "Exhausted max redirects limit (%d), throwing ProtocolException.", redirectCount));
                throw new ProtocolException(String.format("Too many redirects: %d", redirectCount));
            }

            // Build the new request using the redirect info contained in "response".
            request = buildRedirect(response);

            // If we couldn't build the new request, then bail out now and just return our current response;
            if (request == null) {
                break;
            }

            // Send the request to the redirected location and receive the new response.
            response = chain.proceed(request);
        }

        // Return the last response that we received.
        return response;
    }

    /**
     * Builds a new request from a response that indicates that an HTTP redirect
     * should be performed.
     * @param response the response containing the HTTP "redirect"
     * @return a new Request instance or null if an error occurred
     */
    protected Request buildRedirect(Response response) throws ProtocolException {
        // Retrieve the redirected location and validate.
        String location = response.header("Location");
        if (StringUtils.isEmpty(location)) {
            throw new ProtocolException("Location header missing or empty in redirect response");
        }

        LOG.fine(String.format("Redirect location: %s", location));

        // Resolve "location" relative to the original request URL.
        HttpUrl url = resolveUrl(response.request().url(), location);
        if (url == null) {
            throw new ProtocolException(
                    String.format("A redirect response contains an invalid Location header: %s", location));
        }

        // Make sure the original and redirected request URLs have the same scheme.
        boolean sameScheme = url.scheme() == response.request().url().scheme();
        if (!sameScheme) {
            LOG.fine("Redirects to a different URL scheme (http -> https, https -> http) are not supported.");
            return null;
        }

        // Create a request builder from the original request, and start building the redirected request.
        Request.Builder builder = response.request().newBuilder();
        String method = response.request().method();
        if (HttpMethod.permitsRequestBody(method)) {
            // Map the redirected request to a GET if needed.
            int responseCode = response.code();
            boolean maintainBody = responseCode == 307 || responseCode == 308;
            if (maintainBody) {
                // Use the existing method and body.
                builder.method(method, response.request().body());
                LOG.fine(String.format("Using redirect method %s", method));
            } else {
                // Map request to a GET with no body.
                builder.method("GET", null);
                LOG.fine("Redirect method changed to GET.");
            }
            if (!maintainBody) {
                builder.removeHeader("Transfer-Encoding");
                builder.removeHeader("Content-Length");
                builder.removeHeader("Content-Type");
                LOG.fine("Removed body-related headers from redirected request");
            }
        }

        // Finally, if this is not a "safe" redirection request, then we need to
        // strip out any "safe" headers.
        if (!isSafeRedirect(response.request().url(), url)) {
            LOG.fine("This is an unsafe redirect.");
            Set<String> requestHeaders = response.request().headers().names();
            for (String header : safeHeaders) {
                builder.removeHeader(header);

                // If "header" is present in the original request, then log a debug message indicating
                // that we're removing it.
                if (requestHeaders.contains(header)) {
                    LOG.fine(String.format("Removed header '%s' from redirected request.", header));
                }
            }
        } else {
            LOG.fine("This is a safe redirect. No headers will be removed.");
        }

        // Return the new redirected request.
        return builder.url(url).build();
    }

    /**
     * Determine whether or not "statusCode" indicates a redirect.
     *
     * @param statusCode the statusCode to check
     * @return true if the specified status code indicates a redirect, false otherwise
     */
    protected boolean isRedirectStatusCode(int statusCode) {
        boolean isRedirect = false;
        switch (statusCode) {
            case 300: // Multiple Choice
            case 301: // Moved Permanently
            case 302: // Moved Temporarily/Found
            case 303: // See Other
            case 307: // Temporary Redirect
            case 308: // Permanent Redirect
                isRedirect = true;
                break;
        }

        return isRedirect;
    }

    /**
     * Returns true iff the redirection request from "originalUrl" to "redirectedUrl" is
     * considered safe.  In this context, "safe" means:
     * 1. The two URLs share the same host
     * OR
     * 2. The hosts associated with both URLs are located within the IBM Cloud domain (".cloud.ibm.com").
     *
     * @param originalUrl the HttpUrl instance associated with the original request
     * @param redirectedUrl the HttpUrl instance associated with a redirected request
     * @return
     */
    protected boolean isSafeRedirect(HttpUrl originalUrl, HttpUrl redirectedUrl) {
        String origHost = originalUrl.host();
        String redirectHost = redirectedUrl.host();
        boolean sameHost = origHost.equals(redirectHost);
        boolean safeDomain = origHost.endsWith(SAFE_DOMAIN) && redirectHost.endsWith(SAFE_DOMAIN);
        return sameHost || safeDomain;
    }

    /**
     * Returns a new HttpUrl instance by resolving "newUrl" relative to "baseUrl".
     * @param baseUrl a "baseline" HttpUrl instance (e.g. "https://myhost.com/v1")
     * @param newUrl a (possibly relative) URL string (e.g. "../v2/api")
     * @return a new HttpUrl instance obtained by applying "newUrl" to "baseUrl"
     *  (e.g. "https://myhost.com/v2/api")
     */
    protected HttpUrl resolveUrl(HttpUrl baseUrl, String newUrl) {
        HttpUrl result = null;
        try {
            result = baseUrl.newBuilder(newUrl).build();
        } catch (Throwable t) {
            // absorb any exceptions here
        }
        return result;
    }
}
