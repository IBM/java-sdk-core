/**
 * (C) Copyright IBM Corp. 2019, 2021.
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
import java.util.Base64;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.RequestBuilder;

/**
 * This class provides an Authenticator implementation for the "CloudPakForData" environment.
 * This authenticator will use the configured url and other properties to automatically fetch
 * a service instance id access token from the CloudPakForData token service.
 * When the access token expires, a new access token will be fetched.
 *
 * This authenticator uses the "GET /zen-data/v3/service_instances/serviceInstanceId/token" operation
 * supported by the CloudPakForData token service.
 * As such, you can configure either the username, apikey, and serviceInstanceId properties.
 * The url, username, apikey, and serviceInstanceId properties are required.
 */
public class CloudPakForDataServiceInstanceAuthenticator
extends TokenRequestBasedAuthenticator<Cp4dToken, Cp4dServiceInstanceTokenResponse>
  implements Authenticator {

  // Properties specific to a CloudPakForData service instance authenticator.
  private String url;
  private String username;
  private String apikey;
  private String serviceInstanceId;

  /**
   * This Builder class is used to construct CloudPakForDataServiceInstanceAuthenticator instances.
   */
  public static class Builder {
    private String url;
    private String username;
    private String serviceInstanceId;
    private String apikey;
    private boolean disableSSLVerification;
    private Map<String, String> headers;
    private Proxy proxy;
    private okhttp3.Authenticator proxyAuthenticator;

    // Default ctor.
    public Builder() { }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(CloudPakForDataServiceInstanceAuthenticator obj) {
      this.url = obj.url;
      this.username = obj.username;
      this.apikey = obj.apikey;
      this.serviceInstanceId = obj.serviceInstanceId;

      this.disableSSLVerification = obj.getDisableSSLVerification();
      this.headers = obj.getHeaders();
      this.proxy = obj.getProxy();
      this.proxyAuthenticator = obj.getProxyAuthenticator();
    }

    /**
     * Constructs a new instance of CloudPakForDataServiceInstanceAuthenticator from the builder's configuration.
     *
     * @return the CloudPakForDataServiceInstanceAuthenticator instance
     */
    public CloudPakForDataServiceInstanceAuthenticator build() {
      return new CloudPakForDataServiceInstanceAuthenticator(this);
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
     * Sets the apikey property.
     * @param apikey the apikey to use when retrieving an access token
     * @return the Builder
     */
    public Builder apikey(String apikey) {
      this.apikey = apikey;
      return this;
    }

    /**
     * Sets the serviceInstanceId property.
     * @param serviceInstanceId the password to use when retrieving an access token
     * @return the Builder
     */
    public Builder serviceInstanceId(String serviceInstanceId) {
      this.serviceInstanceId = serviceInstanceId;
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
  protected CloudPakForDataServiceInstanceAuthenticator() {
  }

  /**
   * Constructs a CloudPakForDataServiceInstanceAuthenticator instance from the configuration
   * contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected CloudPakForDataServiceInstanceAuthenticator(Builder builder) {
    this.url = builder.url;
    this.username = builder.username;
    this.apikey = builder.apikey;
    this.serviceInstanceId = builder.serviceInstanceId;

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
   * Construct a CloudPakForDataServiceInstanceAuthenticator instance using properties retrieved from the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the CloudPakForDataServiceInstanceAuthenticator instance
   *
   */
  public static CloudPakForDataServiceInstanceAuthenticator fromConfiguration(Map<String, String> config) {
    return new Builder()
      .url(config.get(PROPNAME_URL))
      .username(config.get(PROPNAME_USERNAME))
      .apikey(config.get(PROPNAME_APIKEY))
      .serviceInstanceId(config.get(PROPNAME_SERVICE_INSTANCE_ID))
      .disableSSLVerification(Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue())
      .build();
  }

  /**
   * Initializes the authenticator with all the specified properties.
   * @param url
   *          the base URL associated with the token service.
   *          The path "/zen-data/v3/service_instances/instanceid/token" will be appended to
   *          this value automatically.
   * @param username
   *          the username to be used when retrieving the access token
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param serviceInstanceId
   *          the serviceInstanceId to be used when retrieving the access token
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token service interactions
   */
  protected void init(String url, String username, String apikey, String serviceInstanceId,
    boolean disableSSLVerification, Map<String, String> headers) {
    this.url = url;
    this.username = username;
    this.apikey = apikey;
    this.serviceInstanceId = serviceInstanceId;
    setDisableSSLVerification(disableSSLVerification);
    setHeaders(headers);

    this.validate();
  }

  /**
   * Returns the authentication type associated with this Authenticator.
   * @return the authentication type
   */
  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE;
  }

  /**
   * Validates the configuration of this authenticator.
   */
  @Override
  public void validate() {
    if (StringUtils.isEmpty(this.url)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "url"));
    }

    if (StringUtils.isEmpty(this.username)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "username"));
    }

    if (StringUtils.isEmpty(this.apikey)) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "apikey"));
    }

    if (StringUtils.isEmpty(this.serviceInstanceId)) {
        throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "serviceInstanceId"));
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
   * @return the apikey configured for this authenticator
   */
  public String getApikey() {
    return this.apikey;
  }

  /**
   * @return the service instance id configured for this authenticator
   */
  public String getServiceInstanceId() {
    return this.serviceInstanceId;
  }

  /**
   * Obtains a CP4D access token for the configured authenticator.
   *
   * @return a Cp4dToken instance that contains the access token
   */
  @Override
  public Cp4dToken requestToken() {
    // Generate the authentication url
    String authUrl = String.format("/zen-data/v3/service_instances/%s/token", this.serviceInstanceId);

    // Form a POST request to retrieve the access token.
    RequestBuilder builder = RequestBuilder.get(RequestBuilder.resolveRequestUrl(this.url, authUrl));

    // Add the Content-Type header.
    builder.header(HttpHeaders.AUTHORIZATION, String.join("", "Basic ", getEncodedCredentials()));

    // Invoke the POST request.
    Cp4dToken token;
    try {
      Cp4dServiceInstanceTokenResponse response = invokeRequest(builder, Cp4dServiceInstanceTokenResponse.class);
      token = new Cp4dToken(response);
    } catch (Throwable t) {
      token = new Cp4dToken(t);
    }

    // Construct a new Cp4dToken object from the response and return it.
    return token;
  }

  private String getEncodedCredentials() {
      byte[] credentials = String.join("", this.username, ":", this.apikey).getBytes();
      return Base64.getEncoder().encodeToString(credentials);
  }
}
