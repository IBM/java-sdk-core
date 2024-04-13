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

package com.ibm.cloud.sdk.core.security;

import java.net.Proxy;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.util.RequestUtils;

/**
 * This class provides an Authenticator implementation for the "CloudPakForData" environment.
 * This authenticator will use the configured url and other properties to automatically fetch
 * an access token for service to service authentication from the CloudPakForData service token service.
 * When the access token expires, a new access token will be fetched.
 *
 * This authenticator uses the "GET /v1/service_token"
 * operation supported by the CloudPakForData service token service.
 * As such, you can configure the username, displayName, uid, permissions and expirationTime
 * properties. The url and serviceBrokerSecret properties are required, the rest are optional.
 */
public class CloudPakForDataServiceAuthenticator extends TokenRequestBasedAuthenticator<Cp4dToken, Cp4dTokenResponse>
  implements Authenticator {

  // '/zen-data/internal' must be included in base url
  private static final String SERVICE_TOKEN_URL = "/v1/service_token";

  // Properties specific to a CloudPakForData authenticator.
  private String url;
  // Optional parameters
  private String username;
  private String uid;
  private String displayName;
  private String permissions;
  private String expirationTime;
  // Required parameters
  private String serviceBrokerSecret;

  /**
   * This Builder class is used to construct CloudPakForDataServiceAuthenticator instances.
   */
  public static class Builder {
    private String url;
    // Optional parameters
    private String username;
    private String displayName;
    private String uid;
    private String permissions;
    private String expirationTime;
    // Required parameters
    private String serviceBrokerSecret;

    private boolean disableSSLVerification;
    private Map<String, String> headers;
    private Proxy proxy;
    private okhttp3.Authenticator proxyAuthenticator;

    // Default ctor.
    public Builder() { }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(CloudPakForDataServiceAuthenticator obj) {
      this.url = obj.url;
      this.username = obj.username;
      this.displayName = obj.displayName;
      this.uid = obj.uid;
      this.permissions = obj.permissions;
      this.expirationTime = obj.expirationTime;
      this.serviceBrokerSecret = obj.serviceBrokerSecret;
      this.disableSSLVerification = obj.getDisableSSLVerification();
      this.headers = obj.getHeaders();
      this.proxy = obj.getProxy();
      this.proxyAuthenticator = obj.getProxyAuthenticator();
    }

    /**
     * Constructs a new instance of CloudPakForDataServiceAuthenticator from the builder's configuration.
     *
     * @return the CloudPakForDataServiceAuthenticator instance
     */
    public CloudPakForDataServiceAuthenticator build() {
      return new CloudPakForDataServiceAuthenticator(this);
    }

    /**
     * Sets the url property.
     * @param url the base url to use with the CloudPakForData token service
     * @return the Builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the username property.
     * @param username the username to use when retrieving an access token
     * @return the Builder
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Sets the displayName property.
     * @param displayName the displayName to use when retrieving an access token
     * @return the Builder
     */
    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * Sets the uid property.
     * @param uid the uid to use when retrieving an access token
     * @return the Builder
     */
    public Builder uid(String uid) {
      this.uid = uid;
      return this;
    }

    /**
     * Sets the permissions property.
     * @param permissions the permissions to use when retrieving an access token
     * @return the Builder
     */
    public Builder permissions(String permissions) {
      this.permissions = permissions;
      return this;
    }

    /**
     * Sets the expirationTime property.
     * @param expirationTime the expirationTime to use when retrieving an access token
     * @return the Builder
     */
    public Builder expirationTime(String expirationTime) {
      this.expirationTime = expirationTime;
      return this;
    }

    /**
     * Sets the serviceBrokerSecret property.
     * @param serviceBrokerSecret the serviceBrokerSecret to use when retrieving an access token
     * @return the Builder
     */
    public Builder serviceBrokerSecret(String serviceBrokerSecret) {
      this.serviceBrokerSecret = serviceBrokerSecret;
      return this;
    }

    /**
     * Sets the disableSSLVerification property.
     * @param disableSSLVerification a boolean flag indicating whether or not SSL verification should be disabled
     * when interacting with the CloudPakForData token service
     * @return the Builder
     */
    public Builder disableSSLVerification(boolean disableSSLVerification) {
      this.disableSSLVerification = disableSSLVerification;
      return this;
    }

    /**
     * Sets the headers property.
     * @param headers the set of custom headers to include in requests sent to the CloudPakForData token service
     * @return the Builder
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * Sets the proxy property.
     * @param proxy the java.net.Proxy instance to be used when interacting with the CloudPakForData token service
     * @return the Builder
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Sets the proxyAuthenticator property.
     * @param proxyAuthenticator the okhttp3.Authenticator instance to be used with the proxy when
     * interacting with the CloudPakForData token service
     * @return the Builder
     */
    public Builder proxyAuthenticator(okhttp3.Authenticator proxyAuthenticator) {
      this.proxyAuthenticator = proxyAuthenticator;
      return this;
    }
  }

  // The default ctor is hidden to force the use of the non-default ctors.
  protected CloudPakForDataServiceAuthenticator() {
    setUserAgent(RequestUtils.buildUserAgent("cp4d-service-authenticator"));
}

  /**
   * Constructs a CloudPakForDataServiceAuthenticator instance from the configuration
   * contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected CloudPakForDataServiceAuthenticator(Builder builder) {
    this();
    this.url = builder.url;
    this.username = builder.username;
    this.displayName = builder.displayName;
    this.uid = builder.uid;
    this.permissions = builder.permissions;
    this.expirationTime = builder.expirationTime;
    this.serviceBrokerSecret = builder.serviceBrokerSecret;
    setDisableSSLVerification(builder.disableSSLVerification);
    setHeaders(builder.headers);
    setProxy(builder.proxy);
    setProxyAuthenticator(builder.proxyAuthenticator);

    this.validate();
  }

  /**
   * Returns a new Builder instance pre-loaded with the configuration from "this".
   *
   * @return the builder
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Construct a CloudPakForDataServiceAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the CloudPakForDataServiceAuthenticator instance
   *
   */
  public static CloudPakForDataServiceAuthenticator fromConfiguration(Map<String, String> config) {
    return new Builder()
      .url(config.get(PROPNAME_URL))
      .username(config.get(PROPNAME_USERNAME))
      .displayName(config.get(PROPNAME_DISPLAY_NAME))
      .uid(config.get(PROPNAME_UID))
      .permissions(config.get(PROPNAME_PERMISSIONS))
      .expirationTime(config.get(PROPNAME_EXPIRATION_TIME))
      .serviceBrokerSecret(config.get(PROPNAME_SERVICE_BROKER_SECRET))
      .disableSSLVerification(Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue())
      .build();
  }

  /**
   * Returns the authentication type associated with this Authenticator.
   * @return the authentication type
   */
  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_CP4D_SERVICE;
  }

  /**
   * Validates the configuration of this authenticator.
   */
  @Override
  public void validate() {
    if (StringUtils.isEmpty(this.url)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, PROPNAME_URL));
    }

    // The service broker secret need to be specified.
    if (StringUtils.isEmpty(this.serviceBrokerSecret)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, PROPNAME_SERVICE_BROKER_SECRET));
    }

    // The expiration time must be an integer value
    if (StringUtils.isNotEmpty(this.expirationTime)) {
      try {
          Integer.parseInt(this.expirationTime);
      } catch (Exception e) {
          throw new IllegalArgumentException(String.format(ERRORMSG_PROP_INVALID_INTEGER_VALUE,
               PROPNAME_EXPIRATION_TIME, this.expirationTime));
      }
    }
  }

  /**
   * @return the URL configured for this authenticator
   */
  public String getURL() {
    return this.url;
  }

  /**
   * @return the username configured for this authenticator
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @return the displayName configured for this authenticator
   */
  public String getDisplayName() {
    return this.displayName;
  }

  /**
   * @return the uid configured for this authenticator
   */
  public String getUid() {
    return this.uid;
  }

  /**
   * @return the permissions configured for this authenticator
   */
  public String getPermissions() {
    return this.permissions;
  }

  /**
   * @return the expirationTime configured for this authenticator
   */
  public String getExpirationTime() {
    return this.expirationTime;
  }

  /**
   * @return the serviceBrokerSecret configured for this authenticator
   */
  public String getServiceBrokerSecret() {
    return this.serviceBrokerSecret;
  }

  /**
   * Obtains a CP4D access token for the configured authenticator.
   *
   * @return a Cp4dToken instance that contains the access token
   */
  @Override
  public Cp4dToken requestToken() {
    // Form a GET request to retrieve the service access token.
    RequestBuilder builder = RequestBuilder.get(RequestBuilder.resolveRequestUrl(this.url, SERVICE_TOKEN_URL));
    builder.header(HttpHeaders.USER_AGENT, getUserAgent());

    // Add the secret header containing the zen broker secret
    builder.header("secret", this.serviceBrokerSecret);

    // Add the username if specified.
    if (StringUtils.isNotEmpty(this.username)) {
        builder.query("username", this.username);
    }

    // Add the display name if specified
    if (StringUtils.isNotEmpty(this.displayName)) {
        builder.query("display_name", this.displayName);
    }

    // Add the uid if specified
    if (StringUtils.isNotEmpty(this.uid)) {
        builder.query("uid", this.uid);
    }

    // Add the permissions if specified
    if (StringUtils.isNotEmpty(this.permissions)) {
        builder.query("permissions", this.permissions);
    }

    // Add the expiration time if specified
    if (StringUtils.isNotEmpty(this.expirationTime)) {
        builder.query("expiration_time", this.expirationTime);
    }

    // Invoke the POST request.
    Cp4dToken token;
    try {
      Cp4dTokenResponse response = invokeRequest(builder, Cp4dTokenResponse.class);
      token = new Cp4dToken(response);
    } catch (Throwable t) {
      token = new Cp4dToken(t);
    }

    // Construct a new Cp4dToken object from the response and return it.
    return token;
  }
}
