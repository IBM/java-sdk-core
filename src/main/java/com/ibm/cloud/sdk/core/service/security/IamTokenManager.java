/*
 * Copyright 2018 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.service.security;

import com.google.common.io.BaseEncoding;
import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Request.Builder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Retrieves, stores, and refreshes IAM tokens.
 */
public class IamTokenManager implements Authenticator {
  private String userManagedAccessToken;
  private String apiKey;
  private String url;
  private String clientId;
  private String clientSecret;
  private boolean disableSSLVerification;

  private IamToken tokenData;

  private static final Logger LOG = Logger.getLogger(IamTokenManager.class.getName());
  private static final String ERROR_MESSAGE = "Error getting IAM token from API";
  private static final String DEFAULT_AUTHORIZATION = "Basic Yng6Yng=";
  private static final String DEFAULT_IAM_URL = "https://iam.cloud.ibm.com/identity/token";
  private static final String GRANT_TYPE = "grant_type";
  private static final String REQUEST_GRANT_TYPE = "urn:ibm:params:oauth:grant-type:apikey";
  private static final String API_KEY = "apikey";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String CLOUD_IAM = "cloud_iam";

  public IamTokenManager(IamOptions options) {
    if (options.getApiKey() != null) {
      if (CredentialUtils.hasBadStartOrEndChar(options.getApiKey())) {
        throw new IllegalArgumentException("The IAM API key shouldn't start or end with curly brackets or quotes. "
            + "Please remove any surrounding {, }, or \" characters.");
      }
      this.apiKey = options.getApiKey();
    }
    this.url = (options.getUrl() != null) ? options.getUrl() : DEFAULT_IAM_URL;
    this.userManagedAccessToken = options.getAccessToken();
    this.clientId = options.getClientId();
    this.clientSecret = options.getClientSecret();
    tokenData = new IamToken();
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_IAM;
  }

  @Override
  public void authenticate(Builder builder) {
    // Set the IAM access token as a Bearer Token in the Authorization header.
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
  public String getToken() {
    String token;

    if (userManagedAccessToken != null) {
      // use user-managed access token
      token = userManagedAccessToken;
    } else if (tokenData.getAccessToken() == null || isAccessTokenExpired()) {
      // request new token
      token = requestToken();
    } else {
      // use valid managed token
      token = tokenData.getAccessToken();
    }

    return token;
  }

  /**
   * Request an IAM token using an API key. Also updates internal managed IAM token information.
   *
   * @return the new access token
   */
  private String requestToken() {
    RequestBuilder builder = RequestBuilder.post(RequestBuilder.constructHttpUrl(url, new String[0]));

    builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_FORM_URLENCODED);
    builder.header(HttpHeaders.AUTHORIZATION, getAuthorizationHeaderValue());

    FormBody formBody = new FormBody.Builder()
        .add(GRANT_TYPE, REQUEST_GRANT_TYPE)
        .add(API_KEY, apiKey)
        .add(RESPONSE_TYPE, CLOUD_IAM)
        .build();
    builder.body(formBody);

    tokenData = callIamApi(builder.build());
    return tokenData.getAccessToken();
  }

  /**
   * Check if currently stored access token is expired.
   *
   * Using a buffer to prevent the edge case of the
   * token expiring before the request could be made.
   *
   * The buffer will be a fraction of the total TTL. Using 80%.
   *
   * @return whether the current managed access token is expired or not
   */
  private boolean isAccessTokenExpired() {
    if (tokenData.getExpiresIn() == null || tokenData.getExpiration() == null) {
      return true;
    }

    Double fractionOfTimeToLive = 0.8;
    Long timeToLive = tokenData.getExpiresIn();
    Long expirationTime = tokenData.getExpiration();
    Double refreshTime = expirationTime - (timeToLive * (1.0 - fractionOfTimeToLive));
    Double currentTime = Math.floor(System.currentTimeMillis() / 1000);

    return refreshTime < currentTime;
  }

  /**
   * Executes call to IAM API and returns IamToken object representing the response.
   *
   * @param request the request for the IAM API
   * @return object containing requested IAM token information
   */
  private IamToken callIamApi(final Request request) {
    final IamToken[] returnToken = new IamToken[1];

    Thread iamApiCall = new Thread(new Runnable() {
      @Override
      public void run() {
        Call call = HttpClientSingleton.getInstance().createHttpClient().newCall(request);
        ResponseConverter<IamToken> converter = ResponseConverterUtils.getObject(IamToken.class);

        try {
          okhttp3.Response response = call.execute();

          // handle possible errors
          if (response.code() >= 400) {
            throw new ServiceResponseException(response.code(), response);
          }

          returnToken[0] = converter.convert(response);
        } catch (IOException e) {
          LOG.severe(ERROR_MESSAGE);
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
    });

    iamApiCall.start();
    try {
      iamApiCall.join();
    } catch (InterruptedException e) {
      LOG.severe(ERROR_MESSAGE);
      e.printStackTrace();
    }
    return returnToken[0];
  }

  public String getClientId() {
    return this.clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return this.clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public void setDisableSSLVerification(boolean disableSSLVerification) {
    this.disableSSLVerification = disableSSLVerification;
  }

  public String getAuthorizationHeaderValue() {
    String result;
    if (getClientId() != null && getClientSecret() != null) {
      String s = getClientId() + ":" + getClientSecret();
      result = "Basic " + BaseEncoding.base64().encode(s.getBytes());
    } else {
      result = DEFAULT_AUTHORIZATION;
    }
    return result;
  }
}
