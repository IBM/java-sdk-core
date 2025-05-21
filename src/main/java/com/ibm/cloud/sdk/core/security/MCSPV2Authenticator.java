/**
 * (C) Copyright IBM Corp. 2025..
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

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.util.GsonSingleton;
import com.ibm.cloud.sdk.core.util.RequestUtils;

import okhttp3.OkHttpClient;

/**
 * This class provides an Authenticator implementation for the Multi-Cloud Saas
 * Platform (MCSP) v2 environment.
 * This authenticator invokes the MCSP v2 token-exchange operation
 * (POST /api/2.0/{scopeCollectionType}/{scopeId}/apikeys/token) to obtain an access token for an apikey,
 * and adds the access token to requests via an Authorization header of the form:
 *     "Authorization: Bearer &lt;access-token&gt;"
 * When the access token expires, a new access token will be fetched.
 */
public class MCSPV2Authenticator extends TokenRequestBasedAuthenticatorImmutable<MCSPToken, MCSPV2TokenResponse>
        implements Authenticator {
    private static final Logger LOG = Logger.getLogger(MCSPV2Authenticator.class.getName());
    private static final String OPERATION_PATH = "/api/2.0/{scopeCollectionType}/{scopeId}/apikeys/token";

    // Properties specific to an MCSP authenticator.
    private String apikey;
    private String url;
    private String scopeCollectionType;
    private String scopeId;
    private boolean includeBuiltinActions;
    private boolean includeCustomActions;
    private boolean includeRoles;
    private boolean prefixRoles;
    private Map<String, String> callerExtClaim;

    /**
     * This Builder class is used to construct MCSPV2Authenticator instances.
     */
    public static class Builder {
        private String apikey;
        private String url;
        private String scopeCollectionType;
        private String scopeId;
        private boolean includeBuiltinActions;
        private boolean includeCustomActions;
        private boolean includeRoles;
        private boolean prefixRoles;
        private Map<String, String> callerExtClaim;
        private boolean disableSSLVerification;
        private Map<String, String> headers;
        private Proxy proxy;
        private okhttp3.Authenticator proxyAuthenticator;
        private OkHttpClient client;

        /**
         * Constructs an empty Builder.
         */
        public Builder() {
            // "includeRoles" default value should be true.
            this.includeRoles = true;
        }

        /**
         * Builder ctor which copies config from an existing authenticator instance.
         * @param obj
         */
        private Builder(MCSPV2Authenticator obj) {
            this.apikey = obj.apikey;
            this.url = obj.getURL();
            this.scopeCollectionType = obj.getScopeCollectionType();
            this.scopeId = obj.getScopeId();
            this.includeBuiltinActions = obj.includeBuiltinActions();
            this.includeCustomActions = obj.includeCustomActions();
            this.includeRoles = obj.includeRoles();
            this.prefixRoles = obj.prefixRoles();
            this.callerExtClaim = obj.getCallerExtClaim();
            this.disableSSLVerification = obj.getDisableSSLVerification();
            this.headers = obj.getHeaders();
            this.proxy = obj.getProxy();
            this.proxyAuthenticator = obj.getProxyAuthenticator();
            this.client = obj.getClient();
        }

        /**
         * Constructs a new instance of MCSPAuthenticator from the builder's
         * configuration.
         *
         * @return the MCSPAuthenticator instance
         */
        public MCSPV2Authenticator build() {
            return new MCSPV2Authenticator(this);
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
         * Sets the scopeCollectionType property.
         *
         * @param scopeCollectionType  the scope collection type of item(s). Valid values are:
         * <ul>
         * <li>accounts</li>
         * <li>subscriptions</li>
         * <li>services</li>
         * </ul>
         *
         * @return the Builder
         */
        public Builder scopeCollectionType(String scopeCollectionType) {
            this.scopeCollectionType = scopeCollectionType;
            return this;
        }

        /**
         * Sets the scopeId property.
         *
         * @param scopeId the scope identifier of item(s)
         * @return the Builder
         */
        public Builder scopeId(String scopeId) {
            this.scopeId = scopeId;
            return this;
        }

        /**
         * Sets the includeBuiltinActions property.
         *
         * @param includeBuiltinActions a flag to include builtin actions in the "actions" claim in the
         * MCSP access token (default: false).
         *
         * @return the Builder
         */
        public Builder includeBuiltinActions(boolean includeBuiltinActions) {
            this.includeBuiltinActions = includeBuiltinActions;
            return this;
        }

        /**
         * Sets the includeCustomActions property.
         *
         * @param includeCustomActions a flag to include custom actions in the "actions" claim in the
         * MCSP access token (default: false).
         *
         * @return the Builder
         */
        public Builder includeCustomActions(boolean includeCustomActions) {
            this.includeCustomActions = includeCustomActions;
            return this;
        }

        /**
         * Sets the includeRoles property.
         *
         * @param includeRoles a flag to include the "roles" claim in the MCSP access token (default: true).
         *
         * @return the Builder
         */
        public Builder includeRoles(boolean includeRoles) {
            this.includeRoles = includeRoles;
            return this;
        }

        /**
         * Sets the prefixRoles property.
         *
         * @param prefixRoles a flag to add a prefix with the scope level where the role is defined in the
         * "roles" claim (default: false).
         *
         * @return the Builder
         */
        public Builder prefixRoles(boolean prefixRoles) {
            this.prefixRoles = prefixRoles;
            return this;
        }

        /**
         * Sets the callerExtClaim property.
         *
         * @param callerExtClaim a A map containing keys and values to be injected into the access token as the
         * "callerExt" claim. The keys used in this map must be enabled in the apikey by setting the
         * "callerExtClaimNames" property when the apikey is created.
         * This property is typically only used in scenarios involving an apikey with identityType `SERVICEID`.
         *
         * @return the Builder
         */
        public Builder callerExtClaim(Map<String, String> callerExtClaim) {
            this.callerExtClaim = callerExtClaim;
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

        /**
         * Sets the client property.
         *
         * @param client the OkHttpClient instance that should be used by the authenticator
         * when interacting with the MCSP token service
         * @return the Builder
         */
        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }
    }

    /**
     * The default ctor is "hidden" to force the use of the non-default ctors.
     */
    protected MCSPV2Authenticator() {
        setUserAgent(RequestUtils.buildUserAgent("mcspv2-authenticator"));

        // "includeRoles" default value should be true.
        this.includeRoles = true;
    }

    /**
     * Constructs an MCSPV2Authenticator instance from the configuration contained in
     * "builder".
     *
     * @param builder the Builder instance containing the configuration to be used
     */
    protected MCSPV2Authenticator(Builder builder) {
        this();
        this.apikey = builder.apikey;
        this.url = builder.url;
        this.scopeCollectionType = builder.scopeCollectionType;
        this.scopeId = builder.scopeId;
        this.includeBuiltinActions = builder.includeBuiltinActions;
        this.includeCustomActions = builder.includeCustomActions;
        this.includeRoles = builder.includeRoles;
        this.prefixRoles = builder.prefixRoles;
        this.callerExtClaim = builder.callerExtClaim;
        this._setDisableSSLVerification(builder.disableSSLVerification);
        this._setHeaders(builder.headers);
        this._setProxy(builder.proxy);
        this._setProxyAuthenticator(builder.proxyAuthenticator);
        this._setClient(builder.client);

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
     * Construct an MCSPV2Authenticator instance using properties retrieved from "config".
     *
     * @param config a Map containing the configuration properties
     *
     * @return the MCSPV2Authenticator instance
     */
    public static MCSPV2Authenticator fromConfiguration(Map<String, String> config) {

        // Initialize the builder first with the required properties.
        Builder builder = new Builder()
                .apikey(config.get(PROPNAME_APIKEY))
                .url(config.get(PROPNAME_URL))
                .scopeCollectionType(config.get(PROPNAME_SCOPE_COLLECTION_TYPE))
                .scopeId(config.get(PROPNAME_SCOPE_ID));

        // Now add the optional properties to the builder.
        String strValue;
        Boolean bool;

        strValue = config.get(PROPNAME_INCLUDE_BUILTIN_ACTIONS);
        if (StringUtils.isNotEmpty(strValue)) {
            bool = BooleanUtils.toBooleanObject(strValue);
            if (bool == null) {
                throw new IllegalArgumentException(
                        String.format(ERRORMSG_PROP_INVALID_BOOL, PROPNAME_INCLUDE_BUILTIN_ACTIONS, strValue));
            }
            builder.includeBuiltinActions(bool.booleanValue());
        }

        strValue = config.get(PROPNAME_INCLUDE_CUSTOM_ACTIONS);
        if (StringUtils.isNotEmpty(strValue)) {
            bool = BooleanUtils.toBooleanObject(strValue);
            if (bool == null) {
                throw new IllegalArgumentException(
                        String.format(ERRORMSG_PROP_INVALID_BOOL, PROPNAME_INCLUDE_CUSTOM_ACTIONS, strValue));
            }
            builder.includeCustomActions(bool.booleanValue());
        }

        strValue = config.get(PROPNAME_INCLUDE_ROLES);
        if (StringUtils.isNotEmpty(strValue)) {
            bool = BooleanUtils.toBooleanObject(strValue);
            if (bool == null) {
                throw new IllegalArgumentException(
                        String.format(ERRORMSG_PROP_INVALID_BOOL, PROPNAME_INCLUDE_ROLES, strValue));
            }
            builder.includeRoles(bool.booleanValue());
        }

        strValue = config.get(PROPNAME_PREFIX_ROLES);
        if (StringUtils.isNotEmpty(strValue)) {
            bool = BooleanUtils.toBooleanObject(strValue);
            if (bool == null) {
                throw new IllegalArgumentException(
                        String.format(ERRORMSG_PROP_INVALID_BOOL, PROPNAME_PREFIX_ROLES, strValue));
            }
            builder.prefixRoles(bool.booleanValue());
        }

        strValue = config.get(PROPNAME_DISABLE_SSL);
        if (StringUtils.isNotEmpty(strValue)) {
            bool = BooleanUtils.toBooleanObject(strValue);
            if (bool == null) {
                throw new IllegalArgumentException(
                        String.format(ERRORMSG_PROP_INVALID_BOOL, PROPNAME_DISABLE_SSL, strValue));
            }
            builder.disableSSLVerification(bool.booleanValue());
        }

        strValue = config.get(PROPNAME_CALLER_EXT_CLAIM);
        if (StringUtils.isNotEmpty(strValue)) {
            // Unmarshal the string into a generic Map<String,String>, then set it in the builder.
            Gson gson = GsonSingleton.getGsonWithoutPrettyPrinting();
            Type mapType = new TypeToken<Map<String, String>>() { }.getType();
            Map<String, String> callerExtClaim = gson.fromJson(strValue, mapType);

            builder.callerExtClaim(callerExtClaim);
        }

        return builder.build();
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

        if (StringUtils.isEmpty(this.scopeCollectionType)) {
            throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "scopeCollectionType"));
        }

        if (StringUtils.isEmpty(this.scopeId)) {
            throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "scopeId"));
        }
    }

    @Override
    public String authenticationType() {
        return Authenticator.AUTHTYPE_MCSPV2;
    }

    /**
     * @return the apikey property configured on this Authenticator.
     */
    public String getApiKey() {
        return this.apikey;
    }

    /**
     * @return the url property configured on this Authenticator.
     */
    public String getURL() {
        return this.url;
    }

    /**
     * @return the scopeCollectionType property configured on this Authenticator.
     */
    public String getScopeCollectionType() {
        return this.scopeCollectionType;
    }

    /**
     * @return the scopeId property configured on this Authenticator.
     */
    public String getScopeId() {
        return this.scopeId;
    }

    /**
     * @return the includeBuiltinActions property configured on this Authenticator.
     */
    public boolean includeBuiltinActions() {
        return this.includeBuiltinActions;
    }

    /**
     * @return the includeCustomActions property configured on this Authenticator.
     */
    public boolean includeCustomActions() {
        return this.includeCustomActions;
    }

    /**
     * @return the includeRoles property configured on this Authenticator.
     */
    public boolean includeRoles() {
        return this.includeRoles;
    }

    /**
     * @return the prefixRoles property configured on this Authenticator.
     */
    public boolean prefixRoles() {
        return this.prefixRoles;
    }

    /**
     * @return the callerExtClaim property configured on this Authenticator.
     */
    public Map<String, String> getCallerExtClaim() {
        return this.callerExtClaim;
    }

    /**
     * Fetches an access token for the current authenticator configuration.
     *
     * @return an MCSPToken instance that contains the access token
     */
    @Override
    public MCSPToken requestToken() {
        // Construct a POST request to retrieve the access token from the server.
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("scopeCollectionType", this.getScopeCollectionType());
        pathParams.put("scopeId", this.getScopeId());
        RequestBuilder builder = RequestBuilder
                .post(RequestBuilder.resolveRequestUrl(this.getURL(), OPERATION_PATH, pathParams));

        // Add the request headers.
        builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
        builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_JSON);
        builder.header(HttpHeaders.USER_AGENT, getUserAgent());

        // Add the query params.
        builder.query("includeBuiltinActions", this.includeBuiltinActions());
        builder.query("includeCustomActions", this.includeCustomActions());
        builder.query("includeRoles", this.includeRoles());
        builder.query("prefixRolesWithDefinitionScope", this.prefixRoles());

        // Build the request body and set it on the request builder.
        MCSPV2RequestBody requestBody = new MCSPV2RequestBody(this.getApiKey(), this.getCallerExtClaim());
        builder.bodyContent(HttpMediaType.APPLICATION_JSON, requestBody, null, (InputStream) null);

        // Invoke the POST request.
        MCSPToken token;
        try {
            LOG.log(Level.FINE, "Invoking MCSPv2 token service operation: POST {0}", builder.toUrl());
            MCSPV2TokenResponse response = invokeRequest(builder, MCSPV2TokenResponse.class);
            LOG.log(Level.FINE, "Returned from MCSPv2 token service operation");
            token = new MCSPToken(response);
        } catch (Throwable t) {
            token = new MCSPToken(t);
            LOG.log(Level.FINE, "Exception from MCSPv2 token service operation: ", t);
        }
        return token;
    }

    // This class models the request body supported by the token-exchange operation.
    @SuppressWarnings("unused")
    private static class MCSPV2RequestBody {
      private String apikey;
      private Map<String, String> callerExtClaim;

      MCSPV2RequestBody(String apikey, Map<String, String> callerExtClaim) {
        this.apikey = apikey;
        this.callerExtClaim = callerExtClaim;
      }
    }
}
