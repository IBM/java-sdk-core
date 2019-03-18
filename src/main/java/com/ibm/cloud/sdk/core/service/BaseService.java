/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.service;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpStatus;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.ConflictException;
import com.ibm.cloud.sdk.core.service.exception.ForbiddenException;
import com.ibm.cloud.sdk.core.service.exception.InternalServerErrorException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.service.exception.ServiceUnavailableException;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.exception.UnauthorizedException;
import com.ibm.cloud.sdk.core.service.exception.UnsupportedException;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.cloud.sdk.core.service.security.IamTokenManager;
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import com.ibm.cloud.sdk.core.util.RequestUtils;
import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Abstracts common functionality of various IBM Cloud services.
 */
public abstract class BaseService {

  private static final String BASIC = "Basic ";
  private static final String BEARER = "Bearer ";
  private static final String APIKEY_AS_USERNAME = "apikey";
  private static final String ICP_PREFIX = "icp-";
  private static final Logger LOG = Logger.getLogger(BaseService.class.getName());
  private String apiKey;
  private String username;
  private String password;
  private String endPoint;
  private String defaultEndPoint;
  private final String name;
  private IamTokenManager tokenManager;

  private OkHttpClient client;

  /** The default headers. */
  private Headers defaultHeaders = null;

  /** The skip authentication. */
  private boolean skipAuthentication = false;


  // Regular expression for JSON-related mimetypes.
  protected static final Pattern JSON_MIME_PATTERN =
    Pattern.compile("(?i)application\\/((json)|(merge\\-patch\\+json))(;.*)?");
  protected static final Pattern JSON_PATCH_MIME_PATTERN =
    Pattern.compile("(?i)application\\/json\\-patch\\+json(;.*)?");

  /**
   * Instantiates a new IBM Cloud service.
   *
   * @param name the service name
   */
  public BaseService(final String name) {
    this.name = name;

    // file credentials take precedence
    CredentialUtils.ServiceCredentials fileCredentials = CredentialUtils.getFileCredentials(name);
    if (!fileCredentials.isEmpty()) {
      setCredentialFields(fileCredentials);
    } else {
      setCredentialFields(CredentialUtils.getCredentialsFromVcap(name));
    }

    client = configureHttpClient();
  }

  /**
   * Calls appropriate methods to set credential values based on parsed ServiceCredentials object.
   *
   * @param serviceCredentials object containing parsed credential values
   */
  private void setCredentialFields(CredentialUtils.ServiceCredentials serviceCredentials) {
    setEndPoint(serviceCredentials.getUrl());

    if ((serviceCredentials.getUsername() != null) && (serviceCredentials.getPassword() != null)) {
      setUsernameAndPassword(serviceCredentials.getUsername(), serviceCredentials.getPassword());
    } else if (serviceCredentials.getOldApiKey() != null) {
      setApiKey(serviceCredentials.getOldApiKey());
    }

    if (serviceCredentials.getIamApiKey() != null) {
      IamOptions iamOptions = new IamOptions.Builder()
          .apiKey(serviceCredentials.getIamApiKey())
          .url(serviceCredentials.getIamUrl())
          .build();
      this.tokenManager = new IamTokenManager(iamOptions);
    }
  }

  /**
   * Returns true iff the specified mimeType indicates a JSON-related content type.
   * (e.g. application/json, application/json-patch+json, application/merge-patch+json, etc.).
   * @param mimeType the mimetype to consider
   * @return true if the mimeType indicates a JSON-related content type
   */
  public static boolean isJsonMimeType(String mimeType) {
    return mimeType != null && JSON_MIME_PATTERN.matcher(mimeType).matches();
  }

  /**
   * Returns true iff the specified mimeType indicates a "Json Patch"-related content type.
   * (e.g. application/json-patch+json)).
   * @param mimeType the mimetype to consider
   * @return true if the mimeType indicates a JSON-related content type
   */
  public static boolean isJsonPatchMimeType(String mimeType) {
    return mimeType != null && JSON_PATCH_MIME_PATTERN.matcher(mimeType).matches();
  }

  /**
   * Configure the {@link OkHttpClient}. This method will be called by the constructor and can be used to customize the
   * client that the service will use to perform the http calls.
   *
   * @return the {@link OkHttpClient}
   */
  protected OkHttpClient configureHttpClient() {
    return HttpClientSingleton.getInstance().createHttpClient();
  }

  /**
   * Configures the {@link OkHttpClient} based on the passed-in options.
   *
   * @param options the {@link HttpConfigOptions} object for modifying the client
   */
  public void configureClient(HttpConfigOptions options) {
    client = HttpClientSingleton.getInstance().configureClient(options);
  }

  /**
   * Execute the HTTP request. Okhttp3 compliant.
   *
   * @param request the HTTP request
   *
   * @return the HTTP response
   */
  private Call createCall(final Request request) {
    final Request.Builder builder = request.newBuilder();

    if (request.headers().get(HttpHeaders.USER_AGENT) == null) {
      setUserAgent(builder);
    }
    setDefaultHeaders(builder);
    setAuthentication(builder);

    final Request newRequest = builder.build();
    return client.newCall(newRequest);
  }

  /**
   * Set the User-Agent header.
   *
   * @param builder the Request builder
   */
  private void setUserAgent(final Request.Builder builder) {
    String userAgent = RequestUtils.getUserAgent();
    builder.header(HttpHeaders.USER_AGENT, userAgent);
  }

  /**
   * Sets the default headers.
   *
   * @param builder the new default headers
   */
  protected void setDefaultHeaders(final Request.Builder builder) {
    if (defaultHeaders != null) {
      for (String key : defaultHeaders.names()) {
        builder.header(key, defaultHeaders.get(key));
      }
    }
  }

  /**
   * Creates the service call.
   *
   * @param <T> the generic type
   * @param request the request
   * @param converter the converter
   * @return the service call
   */
  protected final <T> ServiceCall<T> createServiceCall(final Request request, final ResponseConverter<T> converter) {
    final Call call = createCall(request);
    return new WatsonServiceCall<>(call, converter);
  }

  /**
   * Gets the API key.
   *
   *
   * @return the API key
   */
  protected String getApiKey() {
    return apiKey;
  }

  /**
   * Gets the username.
   *
   *
   * @return the username
   */
  protected String getUsername() {
    return username;
  }

  /**
   * Gets the password.
   *
   *
   * @return the password
   */
  protected String getPassword() {
    return password;
  }

  /**
   * Gets the API end point.
   *
   *
   * @return the API end point
   */
  public String getEndPoint() {
    return endPoint;
  }

  /**
   * Checks the status of the tokenManager.
   *
   * @return true if the tokenManager has been set
   */
  public boolean isTokenManagerSet() {
    return tokenManager != null;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the API key.
   *
   * @param apiKey the new API key
   */
  public void setApiKey(String apiKey) {
    if (CredentialUtils.hasBadStartOrEndChar(apiKey)) {
      throw new IllegalArgumentException("The API key shouldn't start or end with curly brackets or quotes. Please "
          + "remove any surrounding {, }, or \" characters.");
    }

    if (this.endPoint.equals(this.defaultEndPoint)) {
      this.endPoint = "https://gateway-a.watsonplatform.net/visual-recognition/api";
    }
    this.apiKey = apiKey;
  }

  /**
   * Sets the authentication. Okhttp3 compliant.
   *
   * @param builder the new authentication
   */
  protected void setAuthentication(final Builder builder) {
    if (tokenManager != null) {
      String accessToken = tokenManager.getToken();
      builder.addHeader(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
    } else if (getApiKey() == null) {
      if (skipAuthentication) {
        return;
      }
      throw new IllegalArgumentException("apiKey or username and password were not specified");
    } else {
      builder.addHeader(HttpHeaders.AUTHORIZATION, apiKey.startsWith(BASIC) ? apiKey : BASIC + apiKey);
    }
  }

  /**
   * Sets the end point.
   *
   * @param endPoint the new end point. Will be ignored if empty or null
   */
  public void setEndPoint(final String endPoint) {
    if (CredentialUtils.hasBadStartOrEndChar(endPoint)) {
      throw new IllegalArgumentException("The URL shouldn't start or end with curly brackets or quotes. Please "
          + "remove any surrounding {, }, or \" characters.");
    }

    if ((endPoint != null) && !endPoint.isEmpty()) {
      String newEndPoint = endPoint.endsWith("/") ? endPoint.substring(0, endPoint.length() - 1) : endPoint;
      if (this.endPoint == null) {
        this.defaultEndPoint = newEndPoint;
      }
      this.endPoint = newEndPoint;
    }
  }

  /**
   * Sets the username and password.
   *
   * @param username the username
   * @param password the password
   */
  public void setUsernameAndPassword(final String username, final String password) {
    if (CredentialUtils.hasBadStartOrEndChar(username) || CredentialUtils.hasBadStartOrEndChar(password)) {
      throw new IllegalArgumentException("The username and password shouldn't start or end with curly brackets or "
          + "quotes. Please remove any surrounding {, }, or \" characters.");
    }

    // we'll perform the token exchange for users UNLESS they're on ICP
    if (username.equals(APIKEY_AS_USERNAME) && !password.startsWith(ICP_PREFIX)) {
      IamOptions iamOptions = new IamOptions.Builder()
          .apiKey(password)
          .build();
      setIamCredentials(iamOptions);
    } else {
      this.username = username;
      this.password = password;
      apiKey = Credentials.basic(username, password);
      clearIamCredentials();
    }
  }

  /**
   * Set the default headers to be used on every HTTP request.
   *
   * @param headers name value pairs of headers
   */
  public void setDefaultHeaders(final Map<String, String> headers) {
    if (headers == null) {
      defaultHeaders = null;
    } else {
      defaultHeaders = Headers.of(headers);
    }
  }

  /**
   * Sets IAM information.
   *
   * Be aware that if you pass in an access token using this method, you accept responsibility for managing the access
   * token yourself. You must set a new access token before this one expires. Failing to do so will result in
   * authentication errors after this token expires.
   *
   * @param iamOptions object containing values to be used for authenticating with IAM
   */
  public void setIamCredentials(IamOptions iamOptions) {
    this.tokenManager = new IamTokenManager(iamOptions);
  }

  private void clearIamCredentials() {
    this.tokenManager = null;
  }
  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder().append(name).append(" [");

    if (endPoint != null) {
      builder.append("endPoint=").append(endPoint);
    }

    return builder.append(']').toString();
  }


  /**
   * Process service call.
   *
   * @param <T> the generic type
   * @param converter the converter
   * @param response the response
   * @return the t
   */
  protected <T> T processServiceCall(final ResponseConverter<T> converter, final Response response) {
    if (response.isSuccessful()) {
      return converter.convert(response);
    }

    switch (response.code()) {
      case HttpStatus.BAD_REQUEST: // HTTP 400
        throw new BadRequestException(response);
      case HttpStatus.UNAUTHORIZED: // HTTP 401
        throw new UnauthorizedException(response);
      case HttpStatus.FORBIDDEN: // HTTP 403
        throw new ForbiddenException(response);
      case HttpStatus.NOT_FOUND: // HTTP 404
        throw new NotFoundException(response);
      case HttpStatus.NOT_ACCEPTABLE: // HTTP 406
        throw new ForbiddenException(response);
      case HttpStatus.CONFLICT: // HTTP 409
        throw new ConflictException(response);
      case HttpStatus.REQUEST_TOO_LONG: // HTTP 413
        throw new RequestTooLargeException(response);
      case HttpStatus.UNSUPPORTED_MEDIA_TYPE: // HTTP 415
        throw new UnsupportedException(response);
      case HttpStatus.TOO_MANY_REQUESTS: // HTTP 429
        throw new TooManyRequestsException(response);
      case HttpStatus.INTERNAL_SERVER_ERROR: // HTTP 500
        throw new InternalServerErrorException(response);
      case HttpStatus.SERVICE_UNAVAILABLE: // HTTP 503
        throw new ServiceUnavailableException(response);
      default: // other errors
        throw new ServiceResponseException(response.code(), response);
    }
  }

  /**
   * Sets the skip authentication.
   *
   * @param skipAuthentication the new skip authentication
   */
  public void setSkipAuthentication(final boolean skipAuthentication) {
    this.skipAuthentication = skipAuthentication;
  }

  /**
   * Defines implementation for modifying and executing service calls.
   *
   * @param <T> the generic type
   */
  class WatsonServiceCall<T> implements ServiceCall<T> {
    private Call call;
    private ResponseConverter<T> converter;

    WatsonServiceCall(Call call, ResponseConverter<T> converter) {
      this.call = call;
      this.converter = converter;
    }

    @Override
    public ServiceCall<T> addHeader(String name, String value) {
      Request.Builder builder = call.request().newBuilder();
      builder.header(name, value);
      call = client.newCall(builder.build());
      return this;
    }

    @Override
    public com.ibm.cloud.sdk.core.http.Response<T> execute() {
      try {
        Response response = call.execute();
        T responseModel = processServiceCall(converter, response);
        return new com.ibm.cloud.sdk.core.http.Response<>(responseModel, response);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void enqueue(final ServiceCallback<? super T> callback) {
      call.enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
          callback.onFailure(e);
        }

        @Override
        public void onResponse(Call call, Response response) {
          try {
            T responseModel = processServiceCall(converter, response);
            callback.onResponse(new com.ibm.cloud.sdk.core.http.Response<>(responseModel, response));
          } catch (Exception e) {
            callback.onFailure(e);
          }
        }
      });
    }

    @Override
    public Single<com.ibm.cloud.sdk.core.http.Response<T>> reactiveRequest() {
      return Single.fromCallable(new Callable<com.ibm.cloud.sdk.core.http.Response<T>>() {
        @Override
        public com.ibm.cloud.sdk.core.http.Response<T> call() throws Exception {
          Response response = call.execute();
          T responseModel = processServiceCall(converter, response);
          return new com.ibm.cloud.sdk.core.http.Response<>(responseModel, response);
        }
      });
    }

    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      if (!call.isExecuted()) {
        final Request r = call.request();
        LOG.warning(r.method() + " request to " + r.url() + " has not been sent. Did you forget to call execute()?");
      }
    }
  }
}