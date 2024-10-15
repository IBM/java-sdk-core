/**
 * (C) Copyright IBM Corp. 2019, 2024.
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
import java.util.logging.Logger;

import okhttp3.OkHttpClient;

/**
 * This class serves as a common base class for Authenticator implementations that interact with a token service
 * via a REST interface.
 * This base class allows for the configuration of the following properties:
 * <ul>
 * <li>disableSSLVerification - a flag that indicates whether or not client-side SSL verification should be disabled.
 * <li>headers - a Map of keys/values that will be set as HTTP headers on requests sent to the token service.
 * <li>proxy - a java.net.Proxy instance that will be set on the OkHttpClient instance used to
 * interact with the token service.
 * <li>proxyAuthenticator - an okhttp3.Authenticator instance to be set on the OkHttpClient instance used to
 * interact with the token service.
 * <li>client - a fully-configured OkHttpClient instance to be used to interact with the token service.
 * </ul>
 */
public abstract class TokenRequestBasedAuthenticator<T extends AbstractToken, R extends TokenServerResponse>
  extends TokenRequestBasedAuthenticatorImmutable<T, R> {

  private static final Logger LOG = Logger.getLogger(TokenRequestBasedAuthenticator.class.getName());

  /**
   * Sets the OkHttpClient instance to be used when interacting with the token service.
   * @param client the OkHttpClient instance to use
   */
  public void setClient(OkHttpClient client) {
    this._setClient(client);
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
}
