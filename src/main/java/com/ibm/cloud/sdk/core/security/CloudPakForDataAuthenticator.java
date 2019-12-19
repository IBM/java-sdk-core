/**
 * (C) Copyright IBM Corp. 2019.
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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.RequestBuilder;

/**
 * This class provides an Authenticator implementation for the "CloudPakForData" environment.
 * This authenticator will use the configured url, username and password values to automatically fetch
 * an access token from the Token Server.
 * When the access token expires, a new access token will be fetched.
 */
public class CloudPakForDataAuthenticator extends TokenRequestBasedAuthenticator<Cp4dToken, Cp4dTokenResponse>
  implements Authenticator {

  // This is the suffix we'll need to add to the user-supplied URL to retrieve an access token.
  private static final String URL_SUFFIX = "/v1/preauth/validateAuth";

  private String url;

  // The default ctor is hidden to force the use of the non-default ctors.
  protected CloudPakForDataAuthenticator() {
  }

  /**
   * Constructs a CloudPakForDataAuthenticator with required properties.
   *
   * @param url
   *          the base URL associated with the token server. The path "/v1/preauth/validateAuth" will be appended to
   *          this value automatically.
   * @param username
   *          the username to be used when retrieving the access token
   * @param password
   *          the password to be used when retrieving the access token
   */
  public CloudPakForDataAuthenticator(String url, String username, String password) {
    init(url, username, password, false, null);
  }

  /**
   * Constructs a CloudPakForDataAuthenticator with all properties.
   *
   * @param url
   *          the base URL associated with the token server. The path "/v1/preauth/validateAuth" will be appended to
   *          this value automatically.
   * @param username
   *          the username to be used when retrieving the access token
   * @param password
   *          the password to be used when retrieving the access token
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   */
  public CloudPakForDataAuthenticator(String url, String username, String password,
    boolean disableSSLVerification, Map<String, String> headers) {
    init(url, username, password, disableSSLVerification, headers);
  }

  /**
   * Construct a CloudPakForDataAuthenticator instance using properties retrieved from the specified Map.
   * @param config a map containing the configuration properties
   */
  public CloudPakForDataAuthenticator(Map<String, String> config) {
    init(config.get(PROPNAME_URL), config.get(PROPNAME_USERNAME),
      config.get(PROPNAME_PASSWORD), Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue(), null);
  }

  /**
   * Initializes the authenticator with all the specified properties.
   * @param url
   *          the base URL associated with the token server. The path "/v1/preauth/validateAuth" will be appended to
   *          this value automatically.
   * @param username
   *          the username to be used when retrieving the access token
   * @param password
   *          the password to be used when retrieving the access token
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   */
  protected void init(String url, String username, String password,
    boolean disableSSLVerification, Map<String, String> headers) {
    this.url = url;
    setBasicAuthInfo(username, password);
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
    return Authenticator.AUTHTYPE_CP4D;
  }

  /**
   * Validates the configuration of this authenticator.
   */
  @Override
  public void validate() {
    if (StringUtils.isEmpty(url)) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "url"));
    }

    if (StringUtils.isEmpty(getUsername())) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "username"));
    }

    if (StringUtils.isEmpty(getPassword())) {
      throw new IllegalArgumentException(String.format(ERRORMSG_PROP_MISSING, "password"));
    }
  }

  /**
   * @return the URL configured for this authenticator.
   */
  public String getURL() {
    return this.url;
  }

  /**
   * Obtains a CP4D access token for the username/password combination using the configured URL.
   *
   * @return a Cp4dToken instance that contains the access token
   */
  @Override
  public Cp4dToken requestToken() {
    // Form a GET request to retrieve the access token.

    // If the user did not include the path on the URL property, then add it now.
    String requestUrl = this.url;
    if (!requestUrl.endsWith(URL_SUFFIX)) {
      requestUrl += URL_SUFFIX;
    }
    requestUrl = requestUrl.replace("//", "/");

    RequestBuilder builder = RequestBuilder.get(RequestBuilder.constructHttpUrl(requestUrl, new String[0]));

    // Invoke the GET request.
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
