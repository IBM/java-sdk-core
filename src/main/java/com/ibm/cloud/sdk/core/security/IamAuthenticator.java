/**
 * (C) Copyright IBM Corp. 2015, 2021.
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

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

import okhttp3.FormBody;

/**
 * This class provides an Authenticator implementation for IAM (Identity and Access Management).
 * This authenticator will use the url and apikey values to automatically fetch
 * an access token from the IAM token service via the "POST /identity/token" operation.
 * When the access token expires, a new access token will be fetched.
 */
public class IamAuthenticator extends TokenRequestBasedAuthenticator<IamToken, IamToken> implements Authenticator {
  private static final String DEFAULT_IAM_URL = "https://iam.cloud.ibm.com";
  private static final String OPERATION_PATH = "/identity/token";
  private static final String GRANT_TYPE = "grant_type";
  private static final String REQUEST_GRANT_TYPE = "urn:ibm:params:oauth:grant-type:apikey";
  private static final String API_KEY = "apikey";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String CLOUD_IAM = "cloud_iam";
  private static final String SCOPE = "scope";

  // Properties specific to an IAM authenticator.
  private String url;
  private String apikey;
  private String scope;
  private String clientId;
  private String clientSecret;

  // This is the value of the Authorization header we'll use when interacting with the token server.
  private String cachedAuthorizationHeader = null;

  /**
   * This Builder class is used to construct IamAuthenticator instances.
   */
  public static class Builder {
    private String url;
    private String apikey;
    private String scope;
    private String clientId;
    private String clientSecret;
    private boolean disableSSLVerification;
    private Map<String, String> headers;
    private Proxy proxy;
    private okhttp3.Authenticator proxyAuthenticator;

    // Default ctor.
    public Builder() { }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(IamAuthenticator obj) {
      this.url = obj.url;
      this.apikey = obj.apikey;
      this.scope = obj.scope;
      this.clientId = obj.clientId;
      this.clientSecret = obj.clientSecret;

      this.disableSSLVerification = obj.getDisableSSLVerification();
      this.headers = obj.getHeaders();
      this.proxy = obj.getProxy();
      this.proxyAuthenticator = obj.getProxyAuthenticator();
    }

    /**
     * Constructs a new instance of IamAuthenticator from the builder's configuration.
     *
     * @return the IamAuthenticator instance
     */
    public IamAuthenticator build() {
      return new IamAuthenticator(this);
    }

    /**
     * Sets the url property.
     * @param url the base url to use with the IAM token service
     * @return the Builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the clientId property.
     * @param clientId the clientId to use when retrieving an access token
     * @return the Builder
     */
    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    /**
     * Sets the clientSecret property.
     * @param clientSecret the clientSecret to use when retrieving an access token
     * @return the Builder
     */
    public Builder clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    /**
     * Sets the apikey property.
     * @param apikey the apikey to use when retrieving an access token
     * @return the Builder
     */
    public Builder apikey(String apikey) {
      this.apikey = apikey;
      return this;
    }

    /**
     * Sets the scope property.
     * @param scope the scope to use when retrieving an access token
     * @return the Builder
     */
    public Builder scope(String scope) {
      this.scope = scope;
      return this;
    }

    /**
     * Sets the disableSSLVerification property.
     * @param disableSSLVerification a boolean flag indicating whether or not SSL verification should be disabled
     * when interacting with the IAM token service
     * @return the Builder
     */
    public Builder disableSSLVerification(boolean disableSSLVerification) {
      this.disableSSLVerification = disableSSLVerification;
      return this;
    }

    /**
     * Sets the headers property.
     * @param headers the set of custom headers to include in requests sent to the IAM token service
     * @return the Builder
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * Sets the proxy property.
     * @param proxy the java.net.Proxy instance to be used when interacting with the IAM token server
     * @return the Builder
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Sets the proxyAuthenticator property.
     * @param proxyAuthenticator the okhttp3.Authenticator instance to be used with the proxy when
     * interacting with the IAM token service
     * @return the Builder
     */
    public Builder proxyAuthenticator(okhttp3.Authenticator proxyAuthenticator) {
      this.proxyAuthenticator = proxyAuthenticator;
      return this;
    }
  }

  // The default ctor is hidden to force the use of the non-default ctors.
  protected IamAuthenticator() {
  }

  /**
   * Constructs an IamAuthenticator instance from the configuration
   * contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected IamAuthenticator(Builder builder) {
    this.url = builder.url;
    this.apikey = builder.apikey;
    this.scope = builder.scope;
    this.clientId = builder.clientId;
    this.clientSecret = builder.clientSecret;

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
   * Constructs an IamAuthenticator with required properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   *
   * @deprecated As of 9.7.0, use IamAuthenticator.Builder() instead
   *
   */
  @Deprecated
  public IamAuthenticator(String apikey) {
    init(apikey, null, null, null, false, null, null);
  }

  /**
   * Constructs an IamAuthenticator with all properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param url
   *          the base URL of the IAM token service (if null/empty, then "https://iam.cloud.ibm.com" is used)
   * @param clientId
   *          the clientId to be used in token server interactions
   * @param clientSecret
   *          the clientSecret to be used in token server interactions
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   *
   * @deprecated As of 9.7.0, use IamAuthenticator.Builder() instead
   *
   */
  @Deprecated
  public IamAuthenticator(String apikey, String url, String clientId, String clientSecret,
    boolean disableSSLVerification, Map<String, String> headers) {
    init(apikey, url, clientId, clientSecret, disableSSLVerification, headers, null);
  }

  /**
   * Constructs an IamAuthenticator with all properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param url
   *          the base URL of the IAM token service (if null/empty, then "https://iam.cloud.ibm.com" is used)
   * @param clientId
   *          the clientId to be used in token server interactions
   * @param clientSecret
   *          the clientSecret to be used in token server interactions
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   * @param scope
   *          the "scope" to use when fetching the bearer token from the IAM token server.
   *          This can be used to obtain an access token with a specific scope.
   *
   * @deprecated As of 9.7.0, use IamAuthenticator.Builder() instead
   *
   */
  @Deprecated
  public IamAuthenticator(String apikey, String url, String clientId, String clientSecret,
    boolean disableSSLVerification, Map<String, String> headers, String scope) {
    init(apikey, url, clientId, clientSecret, disableSSLVerification, headers, scope);
  }

  /**
   * Construct an IamAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @deprecated As of 9.7.0, use IamAuthenticator.Builder() instead
   *
   */
  @Deprecated
  public IamAuthenticator(Map<String, String> config) {
    String apikey = config.get(PROPNAME_APIKEY);
    if (StringUtils.isEmpty(apikey)) {
      apikey = config.get("IAM_APIKEY");
    }
    init(apikey, config.get(PROPNAME_URL),
      config.get(PROPNAME_CLIENT_ID), config.get(PROPNAME_CLIENT_SECRET),
      Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue(), null, config.get(PROPNAME_SCOPE));
  }

  /**
   * Construct a IamAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the IamAuthenticator instance
   */
  public static IamAuthenticator fromConfiguration(Map<String, String> config) {
    // We support both APIKEY and IAM_APIKEY properties for specifying the apikey value.
    String apikey = config.get(PROPNAME_APIKEY);
    if (StringUtils.isEmpty(apikey)) {
      apikey = config.get("IAM_APIKEY");
    }

    return new Builder()
      .url(config.get(PROPNAME_URL))
      .apikey(apikey)
      .scope(config.get(PROPNAME_SCOPE))
      .clientId(config.get(PROPNAME_CLIENT_ID))
      .clientSecret(config.get(PROPNAME_CLIENT_SECRET))
      .disableSSLVerification(Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue())
      .build();
  }

  /**
   * Initializes the authenticator with all the specified properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param url
   *          the base URL of the IAM token service (if null/empty, then "https://iam.cloud.ibm.com" is used)
   * @param clientId
   *          the clientId to be used in token server interactions
   * @param clientSecret
   *          the clientSecret to be used in token server interactions
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   * @param scope
   *          the "scope" to use when fetching the bearer token from the IAM token server.
   *          This can be used to obtain an access token with a specific scope.
   */
  protected void init(String apikey, String url, String clientId, String clientSecret,
    boolean disableSSLVerification, Map<String, String> headers, String scope) {
    this.apikey = apikey;
    this.url = url;
    setClientIdAndSecret(clientId, clientSecret);
    setScope(scope);
    this.validate();

    setDisableSSLVerification(disableSSLVerification);
    setHeaders(headers);
  }

  @Override
  public void validate() {

    if (StringUtils.isEmpty(this.url)) {
      // If no base URL was configured, then use the default IAM base URL.
      this.url = DEFAULT_IAM_URL;
    } else if (this.url.endsWith(OPERATION_PATH)) {
      // Canonicalize the URL by removing the operation path from it if present.
      this.url = this.url.substring(0, this.url.length() - OPERATION_PATH.length());
    }

    if (StringUtils.isEmpty(this.apikey)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "apikey"));
    }

    if (CredentialUtils.hasBadStartOrEndChar(this.apikey)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_INVALID, "apikey"));
    }

    if (StringUtils.isEmpty(getUsername()) && StringUtils.isEmpty(getPassword())) {
      // both empty is ok.
    } else {
      if (StringUtils.isEmpty(getUsername())) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "clientId"));
      }
      if (StringUtils.isEmpty(getPassword())) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "clientSecret"));
      }
    }

    // Assuming everything validates clean, let's cache the basic auth header if
    // the clientId/clientSecret properties are configured.
    this.cachedAuthorizationHeader = constructBasicAuthHeader(this.clientId, this.clientSecret);
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_IAM;
  }

  /**
   * @return the apikey configured on this Authenticator.
   */
  public String getApiKey() {
    return this.apikey;
  }

  /**
   * @return the URL configured on this Authenticator.
   */
  public String getURL() {
    return this.url;
  }

  /**
   * Sets the URL on this Authenticator.
   * @param url the URL representing the IAM token server endpoint
   */
  public void setURL(String url) {
    if (StringUtils.isEmpty(url)) {
      url = DEFAULT_IAM_URL;
    }
    this.url = url;
  }

  /**
   * @return the clientId configured on this Authenticator.
   */
  public String getClientId() {
    return this.clientId;
  }

  /**
   * @return the clientSecret configured on this Authenticator.
   */
  public String getClientSecret() {
    return this.clientSecret;
  }

  /**
   * @return the basic auth username (clientId) configured for this Authenticator.
   *
   * @deprecated As of 9.7.0, use getClientId() instead
   */
  public String getUsername() {
    return getClientId();
  }

  /**
   * @return the basic auth password (clientSecret) configured for this Authenticator.
   *
   * @deprecated As of 9.7.0, use getClientSecret() instead
   */
  public String getPassword() {
    return getClientSecret();
  }

  /**
   * Sets the basic auth username and password values in this Authenticator.
   * These values will be used to build a basic auth Authorization header that will be sent with
   * each request to the token service.
   * @param clientId the basic auth username
   * @param clientSecret the basic auth password
   *
   * @deprecated As of 9.7.0, use IamAuthenticator.setClientIdAndSecret() instead
   */
  @Deprecated
  public void setBasicAuthInfo(String clientId, String clientSecret) {
    setClientIdAndSecret(clientId, clientSecret);
  }

  /**
   * Sets the clientId and clientSecret on this Authenticator.
   * @param clientId the clientId to use in interactions with the token server
   * @param clientSecret the clientSecret to use in interactions with the token server
   */
  public void setClientIdAndSecret(String clientId, String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.validate();
  }

  /**
   * @return the scope parameter
   */
  public String getScope() {
    return this.scope;
  }

  /**
   * Sets the "scope" parameter to use when fetching the bearer token from the IAM token server.
   * @param value a space seperated string that makes up the scope parameter.
   */
  public void setScope(String value) {
    this.scope = value;
  }

  /**
   * Fetches an IAM access token for the apikey using the configured URL.
   *
   * @return an IamToken instance that contains the access token
   */
  @Override
  public IamToken requestToken() {
    // Form a POST request to retrieve the access token.
    RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(this.url, OPERATION_PATH));

    // Now add the Content-Type and (optionally) the Authorization header to the token server request.
    builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_FORM_URLENCODED);
    if (StringUtils.isNotEmpty(this.cachedAuthorizationHeader)) {
      builder.header(HttpHeaders.AUTHORIZATION, this.cachedAuthorizationHeader);
    }

    // Build the form request body.
    FormBody formBody;
    final FormBody.Builder formBodyBuilder = new FormBody.Builder()
        .add(GRANT_TYPE, REQUEST_GRANT_TYPE)
        .add(API_KEY, apikey)
        .add(RESPONSE_TYPE, CLOUD_IAM);
    // Add the scope param if it's not empty
    if (!StringUtils.isEmpty(getScope())) {
      formBodyBuilder.add(SCOPE, getScope());
    }
    formBody = formBodyBuilder.build();
    builder.body(formBody);

    // Invoke the POST request.
    IamToken token;
    try {
      token = invokeRequest(builder, IamToken.class);
    } catch (Throwable t) {
      token = new IamToken(t);
    }
    return token;
  }
}
