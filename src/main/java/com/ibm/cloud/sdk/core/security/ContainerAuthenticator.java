/**
 * (C) Copyright IBM Corp. 2021.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;

import okhttp3.FormBody;

/**
 * ContainerAuthenticator implements an IAM-based authentication scheme whereby it
 * retrieves a "compute resource token" from the local compute resource (VM)
 * and uses that to obtain an IAM access token by invoking the IAM "get token" operation with grant-type=cr-token.
 * The resulting IAM access token is then added to outbound requests in an Authorization header of the form:
 *     Authorization: Bearer &lt;access-token&gt;
 */
public class ContainerAuthenticator extends IamRequestBasedAuthenticator implements Authenticator {
  private static final Logger LOG = Logger.getLogger(ContainerAuthenticator.class.getName());
  private static final String DEFAULT_IAM_URL = "https://iam.cloud.ibm.com";
  private static final String OPERATION_PATH = "/identity/token";
  private static final String DEFAULT_CR_TOKEN_FILENAME = "/var/run/secrets/tokens/vault-token";
  private static final String ERRORMSG_CR_TOKEN_ERROR = "Error reading CR token file: %s";

  // Properties specific to a ContainerAuthenticator.
  private String crTokenFilename;
  private String iamProfileName;
  private String iamProfileId;

  /**
   * This Builder class is used to construct IamAuthenticator instances.
   */
  public static class Builder {
    private String crTokenFilename;
    private String iamProfileName;
    private String iamProfileId;
    private String url;
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
    private Builder(ContainerAuthenticator obj) {
      this.crTokenFilename = obj.crTokenFilename;
      this.iamProfileName = obj.iamProfileName;
      this.iamProfileId = obj.iamProfileId;

      this.url = obj.getURL();
      this.scope = obj.getScope();
      this.clientId = obj.getClientId();
      this.clientSecret = obj.getClientSecret();
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
    public ContainerAuthenticator build() {
      return new ContainerAuthenticator(this);
    }

    /**
     * Sets the crTokenFilename property.
     * @param crTokenFilename the name of the file to use when retrieving the compute resource token
     * from the local compute resource
     * @return the Builder
     */
    public Builder crTokenFilename(String crTokenFilename) {
      this.crTokenFilename = crTokenFilename;
      return this;
    }

    /**
     * Sets the iamProfileName property.
     * @param iamProfileName the name of the linked trusted IAM profile to use when interacting
     * with the IAM token service to obtain the IAM access token.
     * One of 'iamProfileName' or 'iamProfileId' must be specified.
     * @return the Builder
     */
    public Builder iamProfileName(String iamProfileName) {
      this.iamProfileName = iamProfileName;
      return this;
    }

    /**
     * Sets the iamProfileId property.
     * @param iamProfileId the id of the linked trusted IAM profile to use when interacting
     * with the IAM token service to obtain the IAM access token.
     * One of 'iamProfileName' or 'iamProfileId' must be specified.
     * @return the Builder
     */
    public Builder iamProfileId(String iamProfileId) {
      this.iamProfileId = iamProfileId;
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
  protected ContainerAuthenticator() {
  }

  /**
   * Constructs an IamAuthenticator instance from the configuration
   * contained in "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected ContainerAuthenticator(Builder builder) {
    this.crTokenFilename = builder.crTokenFilename;
    this.iamProfileName = builder.iamProfileName;
    this.iamProfileId = builder.iamProfileId;

    setURL(builder.url);
    setScope(builder.scope);
    setClientIdAndSecret(builder.clientId, builder.clientSecret);
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
   * Constructs a ContainerAuthenticator instance using properties contained in the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the ContainerAuthenticator instance
   */
  public static ContainerAuthenticator fromConfiguration(Map<String, String> config) {
    return new Builder()
      .crTokenFilename(config.get(PROPNAME_CR_TOKEN_FILENAME))
      .iamProfileName(config.get(PROPNAME_IAM_PROFILE_NAME))
      .iamProfileId(config.get(PROPNAME_IAM_PROFILE_ID))
      .url(config.get(PROPNAME_URL))
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
      this.setURL(DEFAULT_IAM_URL);
    } else {
      // Canonicalize the URL by removing the operation path from it if present.
      this.setURL(StringUtils.removeEnd(this.getURL(), OPERATION_PATH));
    }

    // At least one of iamProfileName or iamProfileId must be specified.
    if (StringUtils.isEmpty(getIamProfileName()) && StringUtils.isEmpty(getIamProfileId())) {
      throw new IllegalArgumentException(
          String.format(ERRORMSG_ATLEAST_ONE_PROP_ERROR, "iamProfileName", "iamProfileId"));
    }
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_CONTAINER;
  }

  /**
   * @return the crTokenFilename configured on this Authenticator.
   */
  public String getCrTokenFilename() {
    return this.crTokenFilename;
  }

  /**
   * @return the iamProfileName configured on this Authenticator.
   */
  public String getIamProfileName() {
    return this.iamProfileName;
  }

  /**
   * @return the iamProfileId configured on this Authenticator.
   */
  public String getIamProfileId() {
    return this.iamProfileId;
  }

  /**
   * Fetches an IAM access token for the apikey using the configured URL.
   *
   * @return an IamToken instance that contains the access token
   */
  @Override
  public IamToken requestToken() {

    IamToken token;
    try {

      // Retrieve the CR token value for this compute resource.
      String crToken = retrieveCRToken();

      // Form a POST request to retrieve the access token.
      RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(this.getURL(), OPERATION_PATH));

      // Now add the Accept, Content-Type and (optionally) the Authorization header to the
      // token server request.
      builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
      builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_FORM_URLENCODED);
      addAuthorizationHeader(builder);

      // Build the form request body.
      FormBody formBody;
      final FormBody.Builder formBodyBuilder = new FormBody.Builder()
          .add("grant_type", "urn:ibm:params:oauth:grant-type:cr-token")
          .add("cr_token", crToken);

      // We previously verified that one of iamProfileName or iamProfileId are specified
      // so just process them individually here.
      // If both are specified, that's ok too (they must map to the same profile though).
      if (!StringUtils.isEmpty(getIamProfileId())) {
        formBodyBuilder.add("profile_id", getIamProfileId());
      }
      if (!StringUtils.isEmpty(getIamProfileName())) {
        formBodyBuilder.add("profile_name", getIamProfileName());
      }

      // Add the scope param if it's not empty.
      if (!StringUtils.isEmpty(getScope())) {
        formBodyBuilder.add("scope", getScope());
      }

      // Now add the form to the request body.
      formBody = formBodyBuilder.build();
      builder.body(formBody);

      // Invoke the POST request.
      token = invokeRequest(builder, IamToken.class);
    } catch (Throwable t) {
      token = new IamToken(t);
    }
    return token;
  }

  /**
   * Reads the CR token value from the file system.
   * @return the CR token value
   * @throws IllegalStateException
   */
  protected String retrieveCRToken() throws IllegalStateException {
    try {
      // Use the default filename if one wasn't supplied by the user.
      String tokenFilename = getCrTokenFilename();
      if (StringUtils.isEmpty(tokenFilename)) {
        tokenFilename = DEFAULT_CR_TOKEN_FILENAME;
      }

      LOG.log(Level.FINE, "Attempting to read CR token from file: ", tokenFilename);

      // Read the entire file into a byte array, then convert to string.
      byte[] crTokenBytes = Files.readAllBytes(Paths.get(tokenFilename));
      String crToken = new String(crTokenBytes, StandardCharsets.UTF_8);

      LOG.log(Level.FINE, "Successfully read CR token from file: ", tokenFilename);
      return crToken;
    } catch (Throwable t) {
      String msg = (t.getMessage() != null ? t.getMessage() : t.getClass().getName());
      throw new RuntimeException(String.format(ERRORMSG_CR_TOKEN_ERROR, msg), t);
    }
  }
}
