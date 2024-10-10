/**
 * (C) Copyright IBM Corp. 2024.
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

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

/**
 * The IamAssumeAuthenticator obtains an IAM access token for a user-supplied apikey and a trusted profile
 * using the IAM "get-token" operation's "assume" grant type.
 * The authenticator first obtains an initial IAM access token, then exchanges this initial access token for another
 * token that reflects the identity of the trusted profile.
 * When the access token expires, a new access token will be fetched.
 */
public class IamAssumeAuthenticator extends IamRequestBasedAuthenticatorImmutable implements Authenticator {
  private static final Logger LOG = Logger.getLogger(IamAssumeAuthenticator.class.getName());
  private static final String OPERATION_PATH = "/identity/token";

  // Properties specific to an IamAssumeAuthenticator instance.
  private String iamProfileCrn;
  private String iamProfileId;
  private String iamProfileName;
  private String iamAccountId;

  private IamAuthenticator iamDelegate;

  /**
   * This Builder class is used to construct IamAssumeAuthenticator instances.
   */
  public static class Builder {

    // Properties used to build an IamAuthenticator instance.
    private String apikey;
    private String scope;
    private String clientId;
    private String clientSecret;

    // Properties used to build an IamAssumeAuthenticator instance.
    private String iamProfileCrn;
    private String iamProfileId;
    private String iamProfileName;
    private String iamAccountId;

    // Common properties used with both authenticator types.
    private String url;
    private boolean disableSSLVerification;
    private Map<String, String> headers;
    private Proxy proxy;
    private okhttp3.Authenticator proxyAuthenticator;

    private OkHttpClient client;

    // Default ctor.
    public Builder() { }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(IamAssumeAuthenticator obj) {
      if (obj.iamDelegate != null) {
        this.apikey = obj.iamDelegate.getApiKey();
        this.scope = obj.iamDelegate.getScope();
        this.clientId = obj.iamDelegate.getClientId();
        this.clientSecret = obj.iamDelegate.getClientSecret();
      }

      this.iamProfileCrn = obj.getIamProfileCrn();
      this.iamProfileId = obj.getIamProfileId();
      this.iamProfileName = obj.getIamProfileName();
      this.iamAccountId = obj.getIamAccountId();

      this.url = obj.getURL();
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
    public IamAssumeAuthenticator build() {
      return new IamAssumeAuthenticator(this);
    }

    /**
     * Sets the iamProfileCrn property.
     * @param iamProfileCrn iamProfileCrn value to use
     * @return the Builder
     */
    public Builder iamProfileCrn(String iamProfileCrn) {
      this.iamProfileCrn = iamProfileCrn;
      return this;
    }

    /**
     * Sets the iamProfileId property.
     * @param iamProfileId iamProfileId value to use
     * @return the Builder
     */
   public Builder iamProfileId(String iamProfileId) {
      this.iamProfileId = iamProfileId;
      return this;
    }

   /**
    * Sets the iamProfileName property.
    * @param iamProfileName iamProfileName value to use
    * @return the Builder
    */
    public Builder iamProfileName(String iamProfileName) {
      this.iamProfileName = iamProfileName;
      return this;
    }

    /**
     * Sets the iamAccountId property.
     * @param iamAccountId iamAccountId value to use
     * @return the Builder
     */
    public Builder iamAccountId(String iamAccountId) {
      this.iamAccountId = iamAccountId;
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
     * Sets the scope property.
     * @param scope the scope to use when retrieving an access token
     * @return the Builder
     */
    public Builder scope(String scope) {
      this.scope = scope;
      return this;
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

    /**
     * Sets the OkHttpClient instance to be used when interacting with the IAM token service.
     * @param client the OkHttpClient instance to use
     * @return the Builder
     */
    public Builder client(OkHttpClient client) {
      this.client = client;
      return this;
    }
  }

  // The default ctor is hidden to force the use of the non-default ctors.
  protected IamAssumeAuthenticator() {
    setUserAgent(RequestUtils.buildUserAgent("iam-assume-authenticator"));
  }

  /**
   * Constructs an IamAssumeAuthenticator instance from the configuration contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected IamAssumeAuthenticator(Builder builder) {
    this();

    // First, construct our IamAuthenticator delegate.
    IamAuthenticator iamDelegate = new IamAuthenticator.Builder()
        .apikey(builder.apikey)
        .scope(builder.scope)
        .clientId(builder.clientId)
        .clientSecret(builder.clientSecret)
        .url(builder.url)
        .disableSSLVerification(builder.disableSSLVerification)
        .headers(builder.headers)
        .proxy(builder.proxy)
        .proxyAuthenticator(builder.proxyAuthenticator)
        .build();
    this.iamDelegate = iamDelegate;
    iamDelegate.setClient(builder.client);

    this.iamProfileCrn = builder.iamProfileCrn;
    this.iamProfileId = builder.iamProfileId;
    this.iamProfileName = builder.iamProfileName;
    this.iamAccountId = builder.iamAccountId;
    this._setURL(builder.url);
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
   * Construct ann IamAssumeAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the IamAssumeAuthenticator instance
   */
  public static IamAssumeAuthenticator fromConfiguration(Map<String, String> config) {
    // We support both APIKEY and IAM_APIKEY properties for specifying the apikey value.
    String apikey = config.get(PROPNAME_APIKEY);
    if (StringUtils.isEmpty(apikey)) {
      apikey = config.get("IAM_APIKEY");
    }

    return new Builder()
      .iamProfileCrn(config.get(PROPNAME_IAM_PROFILE_CRN))
      .iamProfileId(config.get(PROPNAME_IAM_PROFILE_ID))
      .iamProfileName(config.get(PROPNAME_IAM_PROFILE_NAME))
      .iamAccountId(config.get(PROPNAME_IAM_ACCOUNT_ID))
      .url(config.get(PROPNAME_URL))
      .apikey(apikey)
      .scope(config.get(PROPNAME_SCOPE))
      .clientId(config.get(PROPNAME_CLIENT_ID))
      .clientSecret(config.get(PROPNAME_CLIENT_SECRET))
      .disableSSLVerification(Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue())
      .build();
  }

  @Override
  public void validate() {
    super.validate();

    if (StringUtils.isEmpty(this.getURL())) {
      // If no base URL was configured, then use the default IAM base URL.
      this._setURL(DEFAULT_IAM_URL);
    } else {
      // Canonicalize the URL by removing the operation path from it if present.
      this._setURL(StringUtils.removeEnd(this.getURL(), OPERATION_PATH));
    }

    int numParams = 0;
    if (StringUtils.isNotEmpty(this.getIamProfileCrn())) {
      numParams++;
    }
    if (StringUtils.isNotEmpty(this.getIamProfileId())) {
      numParams++;
    }
    if (StringUtils.isNotEmpty(this.getIamProfileName())) {
      numParams++;
    }

    if (numParams != 1) {
      throw new IllegalArgumentException(
          String.format(ERRORMSG_EXCLUSIVE_PROP_ERROR, "iamProfileCrn, iamProfileId", "iamProfileName"));
    }

    if (StringUtils.isEmpty(this.getIamProfileName()) != StringUtils.isEmpty(this.getIamAccountId())) {
      throw new IllegalArgumentException(ERRORMSG_ACCOUNTID_PROP_ERROR);
    }
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_IAM_ASSUME;
  }

  /**
   * Gets the iamProfileCrn value.
   * @return the iamProfileCrn value
   */
  public String getIamProfileCrn() {
    return iamProfileCrn;
  }

  /**
   * Gets the iamProfileId value.
   * @return the iamProfileId value
   */
  public String getIamProfileId() {
    return iamProfileId;
  }

  /**
   * Gets the iamProfileName value.
   * @return the iamProfileName value
   */
  public String getIamProfileName() {
    return iamProfileName;
  }

  /**
   * Gets the iamAccountId value.
   * @return the iamAccountId value
   */
  public String getIamAccountId() {
    return iamAccountId;
  }

  /**
   * Fetches an IAM access token for the apikey using the configured URL.
   *
   * @return an IamToken instance that contains the access token
   */
  @Override
  public IamToken requestToken() {
    String userAccessToken = this.iamDelegate.getToken();

    // Form a POST request to retrieve the access token.
    RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(this.getURL(), OPERATION_PATH));

    // Now add the Accept, Content-Type, User-Agent and (optionally) the Authorization header
    // to the token server request.
    builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
    builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_FORM_URLENCODED);
    builder.header(HttpHeaders.USER_AGENT, getUserAgent());

    // Build the form request body.
    FormBody formBody;
    final FormBody.Builder formBodyBuilder = new FormBody.Builder()
      .add("grant_type", "urn:ibm:params:oauth:grant-type:assume")
      .add("access_token", userAccessToken);

    if (StringUtils.isNotEmpty(getIamProfileCrn())) {
      formBodyBuilder.add("profile_crn", this.getIamProfileCrn());
    } else if (StringUtils.isNotEmpty(getIamProfileId())) {
      formBodyBuilder.add("profile_id", this.getIamProfileId());
    } else {
      formBodyBuilder.add("profile_name", this.getIamProfileName());
      formBodyBuilder.add("account", this.getIamAccountId());
    }

    formBody = formBodyBuilder.build();
    builder.body(formBody);

    // Invoke the POST request.
    IamToken token;
    try {
      LOG.log(Level.FINE, "Invoking IAM 'get_token (assume)' operation: POST {0}", builder.toUrl());
      token = invokeRequest(builder, IamToken.class);
      LOG.log(Level.FINE, "Returned from IAM 'get_token (assume)' operation");
    } catch (Throwable t) {
      LOG.log(Level.FINE, "Exception from IAM 'get_token (assume)' operation: ", t);
      token = new IamToken(t);
    }
    return token;
  }
}
