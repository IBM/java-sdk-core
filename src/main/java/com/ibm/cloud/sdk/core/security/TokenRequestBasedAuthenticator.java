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

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import org.apache.commons.lang3.StringUtils;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class serves as a common base class for Authenticator implementations that interact with a Token Server
 * via a REST interface.
 * This base class allows for the configuration of the following properties:
 * <ul>
 * <li>username/password - if specified, these values will be used to build a basic auth Authorization header
 * to be sent with requests to the Token Server.
 * <li>disableSSLVerification - a flag that indicates whether or not SSL hostname verification should be disabled.
 * <li>headers - a Map of keys/values that will be set as headers on requests sent to the Token Server.
 * <li>proxy - a java.net.Proxy instance that will be set on the Client object used to interact with the Token Server.
 * <li>proxyAuthenticator - an okhttp3.Authenticator instance to be set on the Client object used to interact wth
 * the Token Server.
 * </ul>
 */
public abstract class TokenRequestBasedAuthenticator<T extends AbstractToken, R extends TokenServerResponse>
  extends AuthenticatorBase implements Authenticator {

  // Configuration properties.
  private String username;
  private String password;
  private boolean disableSSLVerification;
  private Map<String, String> headers;
  private Proxy proxy;
  private okhttp3.Authenticator proxyAuthenticator;

  // This is the user-supplied headers cached in its internal form,
  // ready to add to a Request to the token server.
  private List<Object> cachedUserHeaders = null;

  // This is the value of the Authorization header we'll use when interacting with the token server.
  private String cachedAuthorizationHeader = null;

  // The object which holds the data returned by the Token Server.
  protected T tokenData = null;

  private void setTokenData(T tokenData) {
    this.tokenData = tokenData;
  }

  /**
   * Validates the configuration properties associated with the Authenticator.
   * Each concrete subclass must implement this method.
   */
  @Override
  public abstract void validate();

  /**
   * Returns the authentication type associated with this Authenticator.
   * Each concrete subclass must implement this method.
   * @return the authentication type
   */
  @Override
  public abstract String authenticationType();
  /**
   * @return the basic auth username configured for this Authenticator
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @return the basic auth password configured for this Authenticator
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * Sets the basic auth username and password values in this Authenticator.
   * These values will be used to build a basic auth Authorization header that will be sent with
   * each request to the Token Server.
   * @param username the basic auth username
   * @param password the basic auth password
   */
  public void setBasicAuthInfo(String username, String password) {
    this.username = username;
    this.password = password;
    this.cachedAuthorizationHeader = constructBasicAuthHeader(this.username, this.password);
  }

  /**
   * @return the disableSSLVerification flag
   */
  public boolean getDisableSSLVerification() {
    return disableSSLVerification;
  }

  /**
   * Sets the disableSSLVerification flag.
   * @param disableSSLVerification a flag indicating whether SSL host verification should be disabled
   */
  public void setDisableSSLVerification(boolean disableSSLVerification) {
    this.disableSSLVerification = disableSSLVerification;
  }

  /**
   * Returns the set of user-supplied headers configured for this Authenticator.
   *
   * @return a Map containing the configured headers
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Sets a Map of key/value pairs which will be sent as HTTP headers in any interactions with the Token Server.
   *
   * @param headers
   *          the user-supplied headers to be included in Token Server interactions
   */
  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
    this.cachedUserHeaders = null;

    // Cache the headers in the form used within the outbound requests.
    // The RequestBuilder.header() method accepts a list of Objects which represent the keys and values.
    if (this.headers != null && !this.headers.isEmpty()) {
      this.cachedUserHeaders = new ArrayList<>();
      for (Map.Entry<String, String> header : this.headers.entrySet()) {
        cachedUserHeaders.add(header.getKey());
        cachedUserHeaders.add(header.getValue());
      }
    }
  }

  /**
   * @return the Proxy configured for this Authenticator
   */
  public Proxy getProxy() {
    return proxy;
  }

  /**
   * Sets a Proxy object on this Authenticator.
   * @param proxy the proxy object to be associated with the Client used to interact wth the token server.
   */
  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  /**
   * @return the proxy authenticator configured for this Authenticator
   */
  public okhttp3.Authenticator getProxyAuthenticator() {
    return proxyAuthenticator;
  }

  /**
   * Sets a proxy authenticator on this Authenticator instance.
   * @param proxyAuthenticator the proxy authenticator
   */
  public void setProxyAuthenticator(okhttp3.Authenticator proxyAuthenticator) {
    this.proxyAuthenticator = proxyAuthenticator;
  }

  /**
   * Authenticate the specified request by adding an Authorization header containing a "Bearer" access token.
   */
  @Override
  public void authenticate(Builder builder) {
    String headerValue = constructBearerTokenAuthHeader(getToken());
    if (headerValue != null) {
      builder.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
    }
  }

  /**
   * Builds and invokes the REST request to fetch a new token from the Token Server.
   * Each concrete subclass must implement this method.
   * @return the token object
   */
  public abstract T requestToken();

  /**
   * Calls the extending class' requestToken implementation in a synchronized way. The requestToken implementation
   * will not be called if the stored token has been made valid since this method's initial call.
   *
   * @return the token object
   */
  private synchronized T synchronizedRequestToken() {
    if (this.tokenData != null && this.tokenData.isTokenValid()) {
      return this.tokenData;
    }

    return requestToken();
  }

  /**
   * This function returns the access token fetched from the Token Server.
   * If no token currently exists or the current token has expired, a new token is fetched from the Token Server.
   *
   * @return the access token
   */
  public String getToken() {
    String token;

    if (this.tokenData == null || !this.tokenData.isTokenValid()) {
      setTokenData(synchronizedRequestToken());
    } else if (this.tokenData.needsRefresh()) {

      // Kick off background task to refresh token.
      Thread updateTokenCall = new Thread(new Runnable() {
        @Override
        public void run() {
          setTokenData(requestToken());
        }
      });
      updateTokenCall.start();
    }

    // Make sure we have a non-null tokenData object.
    // This should not occur, but just in case it does... :)
    if (this.tokenData == null) {
      throw new RuntimeException(ERRORMSG_REQ_FAILED + " illegal state: token object not available");
    }

    // Check to see if an exception occurred during our last interaction with the token server.
    if (this.tokenData.getException() != null) {
      throw new RuntimeException(ERRORMSG_REQ_FAILED, tokenData.getException());
    }

    // Return the access token from our stored tokenData object.
    token = tokenData.getAccessToken();

    return token;
  }

  /**
   * Invokes the specified request and returns the response object.
   *
   * @param requestBuilder
   *          the partially-built request for fetching a token from the token server
   * @param responseClass
   *          a Class object which represents the token server response structure
   * @return an instance of the response class R
   * @throws Throwable an error occurred when invoking a token request
   */
  @SuppressWarnings("unchecked")
  protected R invokeRequest(final RequestBuilder requestBuilder, final Class<? extends R> responseClass)
      throws Throwable {

    // Finish building the request to be sent out.

    // If present, set the Authorization header to be sent to the token server.
    if (StringUtils.isNotEmpty(this.cachedAuthorizationHeader)) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION, this.cachedAuthorizationHeader);
    }

    // Now add any user-supplied headers to the request.
    if (this.cachedUserHeaders != null && !this.cachedUserHeaders.isEmpty()) {
      requestBuilder.header(this.cachedUserHeaders.toArray());
    }

    // Allocate the response.
    final Object[] responseObj = new Object[1];

    // Set up the Client we'll use to invoke the request.
    final HttpConfigOptions options = new HttpConfigOptions.Builder()
        .disableSslVerification(this.disableSSLVerification)
        .proxy(this.proxy)
        .proxyAuthenticator(this.proxyAuthenticator)
        .build();
    final OkHttpClient client = HttpClientSingleton.getInstance().configureClient(options);

    final Request request = requestBuilder.build();

    Thread restCall = new Thread(new Runnable() {
      @Override
      public void run() {
        Call call = client.newCall(request);
        ResponseConverter<R> converter = ResponseConverterUtils.getObject(responseClass);

        try {
          okhttp3.Response response = call.execute();

          // handle possible errors
          if (response.code() >= 400) {
            throw new ServiceResponseException(response.code(), response);
          }

          // Store the API response so that we can pass it back to the main thread.
          responseObj[0] = converter.convert(response);
        } catch (Throwable t) {

          // Store the exception so that we can pass it back to the main thread.
          responseObj[0] = t;
        }
      }
    });

    restCall.start();
    try {
      restCall.join();
    } catch (Throwable t) {
      responseObj[0] = t;
    }

    // Check to see if we need to throw an exception now that we're back on the main thread.
    if (responseObj[0] instanceof Throwable) {
      throw (Throwable) responseObj[0];
    }

    return (R) responseObj[0];
  }
}
