/**
 * (C) Copyright IBM Corp. 2023, 2024.
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

package com.ibm.cloud.sdk.core.security;

import java.net.Proxy;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.util.RequestUtils;

/**
 * This class provides an Authenticator implementation for the Multi-Cloud Saas
 * Platform (MCSP) environment. This authenticator will use the url and apikey
 * values to automatically fetch an access token from the MCSP token service via
 * the "POST /siusermgr/api/1.0/apikeys/token" operation. When the access token
 * expires, a new access token will be fetched.
 */
public class MCSPAuthenticator extends TokenRequestBasedAuthenticator<MCSPToken, MCSPTokenResponse>
        implements Authenticator {
    private static final Logger LOG = Logger.getLogger(ContainerAuthenticator.class.getName());
    private static final String OPERATION_PATH = "/siusermgr/api/1.0/apikeys/token";

    // Properties specific to an MCSP authenticator.
    private String apikey;
    private String url;

    /**
     * This Builder class is used to construct MCSPAuthenticator instances.
     */
    public static class Builder {
        private String apikey;
        private String url;
        private boolean disableSSLVerification;
        private Map<String, String> headers;
        private Proxy proxy;
        private okhttp3.Authenticator proxyAuthenticator;

        /**
         * Constructs an empty Builder.
         */
        public Builder() {
        }

        /**
         * Builder ctor which copies config from an existing authenticator instance.
         * @param obj
         */
        private Builder(MCSPAuthenticator obj) {
            this.apikey = obj.apikey;

            this.url = obj.getURL();
            this.disableSSLVerification = obj.getDisableSSLVerification();
            this.headers = obj.getHeaders();
            this.proxy = obj.getProxy();
            this.proxyAuthenticator = obj.getProxyAuthenticator();
        }

        /**
         * Constructs a new instance of MCSPAuthenticator from the builder's
         * configuration.
         *
         * @return the MCSPAuthenticator instance
         */
        public MCSPAuthenticator build() {
            return new MCSPAuthenticator(this);
        }

        /**
         * Sets the apikey property.
         *
         * @param apikey the apikey to use when retrieving an access token
         * @return the Builder
         */
        public Builder apikey(String apikey) {
            this.apikey = apikey;
            return this;
        }

        /**
         * Sets the url property.
         *
         * @param url the base url to use with the MCSP token service
         * @return the Builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the disableSSLVerification property.
         *
         * @param disableSSLVerification a boolean flag indicating whether or not SSL
         *                               verification should be disabled when
         *                               interacting with the MCSP token service
         * @return the Builder
         */
        public Builder disableSSLVerification(boolean disableSSLVerification) {
            this.disableSSLVerification = disableSSLVerification;
            return this;
        }

        /**
         * Sets the headers property.
         *
         * @param headers the set of custom headers to include in requests sent to the
         *                MCSP token service
         * @return the Builder
         */
        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the proxy property.
         *
         * @param proxy the java.net.Proxy instance to be used when interacting with the
         *              MCSP token server
         * @return the Builder
         */
        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the proxyAuthenticator property.
         *
         * @param proxyAuthenticator the okhttp3.Authenticator instance to be used with
         *                           the proxy when interacting with the MCSP token
         *                           service
         * @return the Builder
         */
        public Builder proxyAuthenticator(okhttp3.Authenticator proxyAuthenticator) {
            this.proxyAuthenticator = proxyAuthenticator;
            return this;
        }
    }

    /**
     * The default ctor is "hidden" to force the use of the non-default ctors.
     */
    protected MCSPAuthenticator() {
        setUserAgent(RequestUtils.buildUserAgent("mcsp-authenticator"));
    }

    /**
     * Constructs an MCSPAuthenticator instance from the configuration contained in
     * "builder".
     *
     * @param builder the Builder instance containing the configuration to be used
     */
    protected MCSPAuthenticator(Builder builder) {
        this();
        this.apikey = builder.apikey;
        this.url = builder.url;
        setDisableSSLVerification(builder.disableSSLVerification);
        setHeaders(builder.headers);
        setProxy(builder.proxy);
        setProxyAuthenticator(builder.proxyAuthenticator);

        this.validate();
    }

    /**
     * Returns a new Builder instance pre-loaded with the configuration from "this".
     *
     * @return the Builder instance
     */
    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Construct an MCSPAuthenticator instance using properties retrieved from "config".
     *
     * @param config a Map containing the configuration properties
     *
     * @return the MCSPAuthenticator instance
     */
    public static MCSPAuthenticator fromConfiguration(Map<String, String> config) {
        return new Builder()
                .apikey(config.get(PROPNAME_APIKEY))
                .url(config.get(PROPNAME_URL))
                .disableSSLVerification(Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue())
                .build();
    }

    /**
     * Validates the configuration.
     */
    @Override
    public void validate() {
        if (StringUtils.isEmpty(this.getURL())) {
            throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "url"));
        }

        if (StringUtils.isEmpty(this.apikey)) {
            throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "apikey"));
        }
    }

    @Override
    public String authenticationType() {
        return Authenticator.AUTHTYPE_MCSP;
    }

    /**
     * @return the apikey configured on this Authenticator.
     */
    public String getApiKey() {
        return this.apikey;
    }

    /**
     * @return the url configured on this Authenticator.
     */
    public String getURL() {
        return this.url;
    }

    /**
     * Fetches an access token for the apikey using the configured URL.
     *
     * @return an MCSPToken instance that contains the access token
     */
    @Override
    public MCSPToken requestToken() {
        // Construct a POST request to retrieve the access token from the server.
        RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(this.getURL(), OPERATION_PATH));
        builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
        builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_JSON);
        builder.header(HttpHeaders.USER_AGENT, getUserAgent());

        // Build the request body.
        String requestBody = String.format("{\"apikey\":\"%s\"}", this.getApiKey());
        builder.bodyContent(requestBody, HttpMediaType.APPLICATION_JSON);

        // Invoke the POST request.
        MCSPToken token;
        try {
            LOG.log(Level.FINE, "Invoking MCSP token service operation: POST {0}", builder.toUrl());
            MCSPTokenResponse response = invokeRequest(builder, MCSPTokenResponse.class);
            LOG.log(Level.FINE, "Returned from MCSP token service operation");
            token = new MCSPToken(response);
        } catch (Throwable t) {
            token = new MCSPToken(t);
            LOG.log(Level.FINE, "Exception from MCSP token service operation: ", t);
        }
        return token;
    }
}
