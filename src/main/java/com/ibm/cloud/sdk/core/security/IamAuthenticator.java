/**
 * (C) Copyright IBM Corp. 2015, 2019.
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

import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

import okhttp3.FormBody;

/**
 * This class provides an Authenticator implementation for IAM (Identity and Access Management).
 * This authenticator will use the url and apikey values to automatically fetch
 * an access token from the Token Server.
 * When the access token expires, a new access token will be fetched.
 */
public class IamAuthenticator extends TokenRequestBasedAuthenticator<IamToken, IamToken> implements Authenticator {
  private String apikey;
  private String url;

  private static final String DEFAULT_IAM_URL = "https://iam.cloud.ibm.com/identity/token";
  private static final String GRANT_TYPE = "grant_type";
  private static final String REQUEST_GRANT_TYPE = "urn:ibm:params:oauth:grant-type:apikey";
  private static final String API_KEY = "apikey";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String CLOUD_IAM = "cloud_iam";

  // The default ctor is hidden to force the use of the non-default ctors.
  protected IamAuthenticator() {
  }

  /**
   * Constructs an IamAuthenticator with required properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   */
  public IamAuthenticator(String apikey) {
    init(apikey, null, null, null, false, null);
  }

  /**
   * Constructs an IamAuthenticator with all properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param url
   *          the URL representing the token server endpoint
   * @param clientId
   *          the clientId to be used in token server interactions
   * @param clientSecret
   *          the clientSecret to be used in token server interactions
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   */
  public IamAuthenticator(String apikey, String url, String clientId, String clientSecret,
    boolean disableSSLVerification, Map<String, String> headers) {
    init(apikey, url, clientId, clientSecret, disableSSLVerification, headers);
  }

  /**
   * Construct an IamAuthenticator instance using properties retrieved from the specified Map.
   * @param config a map containing the configuration properties
   */
  public IamAuthenticator(Map<String, String> config) {
    String apikey = config.get(PROPNAME_APIKEY);
    if (StringUtils.isEmpty(apikey)) {
      apikey = config.get("IAM_APIKEY");
    }
    init(apikey, config.get(PROPNAME_URL),
      config.get(PROPNAME_CLIENT_ID), config.get(PROPNAME_CLIENT_SECRET),
      Boolean.valueOf(config.get(PROPNAME_DISABLE_SSL)).booleanValue(), null);
  }

  /**
   * Initializes the authenticator with all the specified properties.
   *
   * @param apikey
   *          the apikey to be used when retrieving the access token
   * @param url
   *          the URL representing the token server endpoint
   * @param clientId
   *          the clientId to be used in token server interactions
   * @param clientSecret
   *          the clientSecret to be used in token server interactions
   * @param disableSSLVerification
   *          a flag indicating whether SSL hostname verification should be disabled
   * @param headers
   *          a set of user-supplied headers to be included in token server interactions
   */
  protected void init(String apikey, String url, String clientId, String clientSecret,
    boolean disableSSLVerification, Map<String, String> headers) {
    this.apikey = apikey;
    if (StringUtils.isEmpty(url)) {
      url = DEFAULT_IAM_URL;
    }
    this.url = url;
    setDisableSSLVerification(disableSSLVerification);
    setHeaders(headers);
    setClientIdAndSecret(clientId, clientSecret);
  }

  @Override
  public void validate() {
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
    return getUsername();
  }

  /**
   * @return the clientSecret configured on this Authenticator.
   */
  public String getClientSecret() {
    return getPassword();
  }

  /**
   * Sets the clientId and clientSecret on this Authenticator.
   * @param clientId the clientId to use in interactions with the token server
   * @param clientSecret the clientSecret to use in interactions with the token server
   */
  public void setClientIdAndSecret(String clientId, String clientSecret) {
    setBasicAuthInfo(clientId, clientSecret);
    this.validate();
  }

  /**
   * Fetches an IAM access token for the apikey using the configured URL.
   *
   * @return an IamToken instance that contains the access token
   */
  @Override
  public IamToken requestToken() {
    RequestBuilder builder = RequestBuilder.post(RequestBuilder.constructHttpUrl(url, new String[0]));

    // Now add the Content-Type and (optionally) the Authorization header to the token server request.
    builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_FORM_URLENCODED);
    FormBody formBody = new FormBody.Builder()
        .add(GRANT_TYPE, REQUEST_GRANT_TYPE)
        .add(API_KEY, apikey)
        .add(RESPONSE_TYPE, CLOUD_IAM)
        .build();
    builder.body(formBody);

    IamToken token;
    try {
      token = invokeRequest(builder, IamToken.class);
    } catch (Throwable t) {
      token = new IamToken(t);
    }
    return token;
  }
}
