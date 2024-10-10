/**
 * (C) Copyright IBM Corp. 2015, 2024.
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

import okhttp3.OkHttpClient;

/**
 * This class effectively adds some setter functions to the IamRequestBasedAuthenticatorImmutable class in order
 * to allow subclasses (e.g. IamAuthenticator, ContainerAuthenticator) to be mutable after construction.
 */
public abstract class IamRequestBasedAuthenticator extends IamRequestBasedAuthenticatorImmutable
  implements Authenticator {

  /**
   * Sets the URL on this Authenticator.
   * @param url the URL representing the IAM token server endpoint
   */
  public void setURL(String url) {
    this._setURL(url);
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
   * Sets the "scope" parameter to use when fetching the bearer token from the IAM token server.
   * @param value a space seperated string that makes up the scope parameter.
   */
  public void setScope(String value) {
    this.scope = value;
  }

  /**
   * Sets the disableSSLVerification flag.
   * @param disableSSLVerification a flag indicating whether SSL host verification should be disabled
   */
  public void setDisableSSLVerification(boolean disableSSLVerification) {
    this._setDisableSSLVerification(disableSSLVerification);
  }

  /**
   * Sets a Map of key/value pairs which will be sent as HTTP headers in any interactions with the token service.
   *
   * @param headers
   *          the user-supplied headers to be included in token service interactions
   */
  public void setHeaders(Map<String, String> headers) {
    this._setHeaders(headers);
  }

  /**
   * Sets a Proxy object on this Authenticator.
   * @param proxy the proxy object to be associated with the Client used to interact with the token service.
   */
  public void setProxy(Proxy proxy) {
    this._setProxy(proxy);
  }

  /**
   * Sets a proxy authenticator on this Authenticator instance.
   * @param proxyAuthenticator the proxy authenticator
   */
  public void setProxyAuthenticator(okhttp3.Authenticator proxyAuthenticator) {
    this._setProxyAuthenticator(proxyAuthenticator);
  }

  /**
   * Sets the OkHttpClient instance to be used when interacting with the token service.
   * @param client the OkHttpClient instance to use
   */
  public void setClient(OkHttpClient client) {
    this._setClient(client);
  }
}
