/**
 * Copyright 2019 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.security.icp4d;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import com.ibm.cloud.sdk.security.Authenticator;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;

/**
 * This class implements support for the ICP4D authentication mechanism.
 */
public class ICP4DAuthenticator implements Authenticator {
  private static final Logger LOG = Logger.getLogger(ICP4DAuthenticator.class.getName());

  // This is the suffix we'll need to add to the user-supplied URL to retrieve an access token.
  private static final String URL_SUFFIX = "/v1/preauth/validateAuth";

  private static final String ERROR_MSG = "Error while retrieving access token from ICP4D token service: ";

  // Configuration properties for this authenticator.
  private ICP4DConfig config;

  // This field holds an access token and its expiration time.
  private ICP4DToken tokenData;

  // Hide the default ctor to force the use of the 1-arg ctor.
  private ICP4DAuthenticator() {
  }

  public ICP4DAuthenticator(ICP4DConfig config) {
    this.config = config;
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_ICP4D;
  }

  /**
   * Authenticate the specified request by adding an Authorization header containing a Bearer token.
   */
  @Override
  public void authenticate(Builder builder) {
      builder.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getToken());
  }

  /**
   * This function returns an access token. The source of the token is determined by the following logic:
   * 1. If user provides their own managed access token, assume it is valid and send it
   * 2. If this class is managing tokens and does not yet have one, or the token is expired, make a request
   * for one
   * 3. If this class is managing tokens and has a valid token stored, send it
   *
   * @return the valid access token
   */
  protected String getToken() {
    String token;

    if (StringUtils.isNotEmpty(config.getUserManagedAccessToken())) {
      // If the user set their own access token, then use it.
      token = config.getUserManagedAccessToken();
    } else {
      // Request a new token if necessary.
      if (this.tokenData == null || !this.tokenData.isTokenValid()) {
        this.tokenData = requestToken();
      }

      // Return the access token from our ICP4DToken object.
      token = this.tokenData.accessToken;
    }

    return token;
  }

  /**
   * Obtains an ICP4D access token for the username/password combination using the configured URL.
   * @return an ICP4DToken instance that contains the access token
   */
  protected ICP4DToken requestToken() {
    // Form a GET request to retrieve the access token.
    String requestUrl = config.getUrl() + URL_SUFFIX;
    requestUrl = requestUrl.replace("//", "/");
    RequestBuilder builder = RequestBuilder.get(RequestBuilder.constructHttpUrl(requestUrl, new String[0]));
    builder.header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader());

    // Invoke the GET request.
    ICP4DTokenResponse response = invokeRequest(builder.build());

    // Construct a new ICP4DToken object from the response and return it.
    return new ICP4DToken(response);
  }

  private String buildBasicAuthHeader() {
    return "Basic "
        + Base64.getEncoder().encodeToString((this.config.getUsername() + ":" + this.config.getPassword()).getBytes());
  }

  /**
   * Executes the specified request and returns the response object containing the ICP4D token.
   *
   * @param request the request for obtaining an ICP4D access token
   * @return an ICP4DTokenResponse instance that contains the requested access token and related info
   */
  private ICP4DTokenResponse invokeRequest(final Request request) {
    final ICP4DTokenResponse[] returnToken = new ICP4DTokenResponse[1];
    final boolean disableSSL = this.config.isDisableSSLVerification();

    Thread restCall = new Thread(new Runnable() {
      @Override
      public void run() {
        // Initialize a client with the correct SSL handling set up.
        OkHttpClient client = HttpClientSingleton.getInstance().createHttpClient();
        if (disableSSL) {
          HttpConfigOptions httpOptions = new HttpConfigOptions.Builder().disableSslVerification(true).build();
          client = HttpClientSingleton.getInstance().configureClient(httpOptions);
        }
        Call call = client.newCall(request);
        ResponseConverter<ICP4DTokenResponse> converter = ResponseConverterUtils.getObject(ICP4DTokenResponse.class);

        try {
          okhttp3.Response response = call.execute();

          // handle possible errors
          if (response.code() >= 400) {
            throw new ServiceResponseException(response.code(), response);
          }

          returnToken[0] = converter.convert(response);
        } catch (IOException e) {
          throw new RuntimeException(ERROR_MSG, e);
        }
      }
    });

    restCall.start();
    try {
      restCall.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(ERROR_MSG, e);
    }
    return returnToken[0];
  }
}
