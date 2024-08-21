/**
 * (C) Copyright IBM Corp. 2015, 2022.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions.LoggingLevel;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpStatus;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.ConflictException;
import com.ibm.cloud.sdk.core.service.exception.ForbiddenException;
import com.ibm.cloud.sdk.core.service.exception.InternalServerErrorException;
import com.ibm.cloud.sdk.core.service.exception.InvalidServiceResponseException;
import com.ibm.cloud.sdk.core.service.exception.NotAcceptableException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.service.exception.ServiceUnavailableException;
import com.ibm.cloud.sdk.core.service.exception.TooManyRequestsException;
import com.ibm.cloud.sdk.core.service.exception.UnauthorizedException;
import com.ibm.cloud.sdk.core.service.exception.UnsupportedException;
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import com.ibm.cloud.sdk.core.util.RequestUtils;

import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

import javax.net.ssl.SSLHandshakeException;

/**
 * Abstracts common functionality of various IBM Cloud services.
 */
public abstract class BaseService {
  private static final Logger LOG = Logger.getLogger(BaseService.class.getName());
  public static final String PROPNAME_URL = "URL";
  public static final String PROPNAME_DISABLE_SSL = "DISABLE_SSL";
  public static final String PROPNAME_ENABLE_GZIP = "ENABLE_GZIP";
  public static final String PROPNAME_ENABLE_RETRIES = "ENABLE_RETRIES";
  public static final String PROPNAME_MAX_RETRIES = "MAX_RETRIES";
  public static final String PROPNAME_RETRY_INTERVAL = "RETRY_INTERVAL";

  private static final String ERRORMSG_NO_AUTHENTICATOR = "Authentication information was not properly configured.";
  private static final String ERRORMSG_SSL = "The connection failed because the SSL certificate is not valid. To use"
      + " a self-signed certificate, set the disableSslVerification parameter in HttpConfigOptions.";

  private String serviceUrl;
  private final String name;
  private Authenticator authenticator;

  private OkHttpClient client;

  /** The default headers. */
  private Headers defaultHeaders = null;

  // Regular expression for JSON-related mimetypes.
  protected static final Pattern JSON_MIME_PATTERN =
    Pattern.compile("(?i)application\\/((json)|(merge\\-patch\\+json))(\\s*;.*)?");
  protected static final Pattern JSON_PATCH_MIME_PATTERN =
    Pattern.compile("(?i)application\\/json\\-patch\\+json(\\s*;.*)?");

  // Hide the default ctor to prevent clients from calling it directly.
  protected BaseService() {
    name = null;
  }

  /**
   * Instantiates a new IBM Cloud service.
   *
   * @param name the service name
   * @param authenticator an Authenticator instance that will perform authentication on outgoing requests
   */
  public BaseService(final String name, Authenticator authenticator) {
    this.name = name;

    if (authenticator == null) {
      throw new IllegalArgumentException(ERRORMSG_NO_AUTHENTICATOR);
    }
    this.authenticator = authenticator;

    // Configure a default client instance.
    this.client = configureHttpClient();

    // If BaseService's logger is configured for FINE logging or lower (lower numbers mean more detail),
    // then configure the HTTP client to do HTTP message logging.
    if (LOG.isLoggable(Level.FINE)) {
      HttpConfigOptions options = new HttpConfigOptions.Builder()
          .loggingLevel(LoggingLevel.BODY)
          .build();
      this.configureClient(options);
    }
  }

  public void configureService(String serviceName) {
    if (serviceName == null || serviceName.isEmpty()) {
      throw new IllegalArgumentException("Error configuring service. Service name is required.");
    }

    LOG.log(Level.FINE, "Configuring BaseService instance using service name: {0}", serviceName);

    // Try to retrieve the service URL from either a credential file, environment, or VCAP_SERVICES.
    Map<String, String> props = CredentialUtils.getServiceProperties(serviceName);
    String url = props.get(PROPNAME_URL);
    if (StringUtils.isNotEmpty(url)) {
      this.setServiceUrl(url);
    }

    // Check to see if "disable ssl" was set in the service properties.
    Boolean disableSSL = Boolean.valueOf(props.get(PROPNAME_DISABLE_SSL));
    if (disableSSL) {
      HttpConfigOptions options = new HttpConfigOptions.Builder()
          .disableSslVerification(true)
          .build();
      this.configureClient(options);
      LOG.log(Level.FINE, "Disabled SSL verification");
    }
    // Check to see if "enable gzip" was set in the service properties.
    String s = props.get(PROPNAME_ENABLE_GZIP);
    if (StringUtils.isNotEmpty(s)) {
      Boolean enableGzipCompression = Boolean.valueOf(s);
      enableGzipCompression(enableGzipCompression);
    }

    // Check to see if "ENABLE_RETRIES" was set in the service properties.
    // If it is set to true, then we'll also try to retrieve "MAX_RETRIES" and "RETRY_INTERVAL".
    Boolean enableRetries = Boolean.valueOf(props.get(PROPNAME_ENABLE_RETRIES));
    if (enableRetries) {
      // The default number of the maximum retries is 4.
      int maxRetries = 4;
      // Set the max retry interval to 30 seconds.
      int maxRetryInterval = 30;

      try {
        maxRetries = Integer.valueOf(props.get(PROPNAME_MAX_RETRIES));
      } catch (NumberFormatException e) {
        LOG.log(Level.WARNING, "Non-numeric MAX_RETRIES value.");
      }

      try {
        maxRetryInterval = Integer.valueOf(props.get(PROPNAME_RETRY_INTERVAL));
      } catch (NumberFormatException e) {
        LOG.log(Level.WARNING, "Non-numeric RETRY_INTERVAL value.");
      }

      enableRetries(maxRetries, maxRetryInterval);
    }
  }

  /**
   * Enables gzip compression of requests bodies for the current client. If shouldEnableCompression
   * is true, then a new client is configured with the GzipRequestInterceptor.
   *
   * @param shouldEnableCompression the value used to set the enableGzipCompression HttpConfigOption
   */
  public void enableGzipCompression(boolean shouldEnableCompression) {
    HttpConfigOptions options = new HttpConfigOptions.Builder()
        .enableGzipCompression(shouldEnableCompression)
        .build();
    this.configureClient(options);
    String verb = shouldEnableCompression ? "Enabled" : "Disabled";
    LOG.log(Level.FINE, "{0} GZIP compression in HTTP client", verb);
  }

  /**
   * Enables the retries for the HTTP requests.
   *
   * @param maxRetries the value of the maximum number of retries
   * @param maxRetryInterval the maximum time to wait between retries in seconds
   */
  public void enableRetries(int maxRetries, int maxRetryInterval) {
    HttpConfigOptions options = new HttpConfigOptions.Builder()
      .enableRetries(this.authenticator, maxRetries, maxRetryInterval)
      .build();
    this.configureClient(options);
    LOG.log(Level.FINE, "Enabled retries; maxRetries={0}, maxRetryInterval={1}",
        new Object[] { maxRetries, maxRetryInterval });
  }

  /**
   * Disables the retries for the HTTP requests.
   */
  public void disableRetries() {
    HttpConfigOptions options = new HttpConfigOptions.Builder()
      .disableRetries()
      .build();
    this.configureClient(options);
    LOG.log(Level.FINE, "Disabled retries");
  }

  /**
   * Returns the currently-configured {@link OkHttpClient} instance.
   * @return the {@link OkHttpClient} instance
   */
  public OkHttpClient getClient() {
    return client;
  }

  /**
   * Sets a new {@link OkHttpClient} instance to be used for API invocations by this BaseService instance.
   * @param client the new {@link OkHttpClient} instance
   */
  public void setClient(OkHttpClient client) {
    this.client = client;
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
   * Constructs a service URL by formatting a parameterized URL.
   *
   * @param parameterizedUrl URL that contains variable placeholders, e.g. "{scheme}://ibm.com".
   * @param defaultUrlVariables map from variable names to default values.
   *  Each variable in the parameterized URL must have a default value specified in this map.
   * @param providedUrlVariables map from variable names to desired values.
   *  If a variable is not provided in this map,
   *  the default variable value will be used instead.
   * @return the formatted URL with all variable placeholders replaced by values.
   */
  public static String constructServiceUrl(
    String parameterizedUrl,
    Map<String, String> defaultUrlVariables,
    Map<String, String> providedUrlVariables
  ) {

    // If null was passed, we set the variables to an empty map.
    // This results in all default variable values being used.
    if (providedUrlVariables == null) {
      providedUrlVariables = new HashMap<>();
    }

    // Verify the provided variable names.
    for (String name : providedUrlVariables.keySet()) {
      if (!defaultUrlVariables.containsKey(name)) {
        throw new IllegalArgumentException(
          String.format(
            "'%s' is an invalid variable name."
            + "\nValid variable names: %s.",
            name,
            // Display the variable names in alphabetical order.
            // This allows for testing the full exception message.
            defaultUrlVariables.keySet().stream().sorted().collect(Collectors.toList())
          )
        );
      }
    }

    // Format the URL with provided or default variable values.
    String formattedUrl = parameterizedUrl;

    for (Map.Entry<String, String> defaultEntry : defaultUrlVariables.entrySet()) {
      String name = defaultEntry.getKey();
      String defaultValue = defaultEntry.getValue();

      // Use the default variable value if none was provided.
      String providedValue = providedUrlVariables.get(name);
      String formatValue = providedValue != null ? providedValue : defaultValue;

      formattedUrl = formattedUrl.replaceAll("\\{" + name + "}", formatValue);
    }
    return formattedUrl;
  }

  /**
   * Constructs a service URL by formatting a parameterized URL.
   *
   * @param parameterizedUrl URL that contains variable placeholders, e.g. "{scheme}://ibm.com".
   *
   * @param defaultUrlVariables map from variable names to default values.
   *  Each variable in the parameterized URL must have a default value specified in this map.
   *
   * @param providedUrlVariables map from variable names to desired values.
   *  If a variable is not provided in this map,
   *  the default variable value will be used instead.
   *
   * @return the formatted URL with all variable placeholders replaced by values.
   *
   * @deprecated use constructServiceUrl() instead.
   */
  @Deprecated
  public static String constructServiceURL(
    String parameterizedUrl,
    Map<String, String> defaultUrlVariables,
    Map<String, String> providedUrlVariables
  ) {
    return BaseService.constructServiceUrl(parameterizedUrl, defaultUrlVariables, providedUrlVariables);
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
   * Configures the currently-configured {@link OkHttpClient} instance based on the passed-in options.
   *
   * @param options the {@link HttpConfigOptions} object for modifying the client
   */
  public void configureClient(HttpConfigOptions options) {
    this.client = HttpClientSingleton.getInstance().configureClient(this.client, options);
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
  protected <T> ServiceCall<T> createServiceCall(final Request request, final ResponseConverter<T> converter) {
    final Call call = createCall(request);
    return new IBMCloudSDKServiceCall<>(call, converter);
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
   * Sets the authentication information on the specified request builder.
   * This method will invoke the configured Authenticator instance to perform the
   * authentication needed for that authentication type.   This could involve adding an
   * Authorization header, or perhaps adding a specific query param to the request URL.
   *
   * @param builder the request builder that represents the outgoing requst on which
   * the authentication information should be set.
   */
  protected void setAuthentication(final Builder builder) {
    if (this.authenticator != null) {
      this.authenticator.authenticate(builder);
    } else {
      throw new IllegalArgumentException(ERRORMSG_NO_AUTHENTICATOR);
    }
  }

  /**
   * Gets the API end point.
   *
   *
   * @return the API end point
   * @deprecated Use getServiceURL() instead.
   */
  @Deprecated
  public String getEndPoint() {
    return this.getServiceUrl();
  }

  /**
   * Sets the end point.
   *
   * @param endPoint the new end point. Will be ignored if empty or null
   * @deprecated Use setServiceURL() instead.
   */
  @Deprecated
  public void setEndPoint(final String endPoint) {
    this.setServiceUrl(endPoint);
  }

  /**
   * Returns the set of default headers current set on this BaseService instance.
   * @return the Headers object containing the default headers.
   */
  public Headers getDefaultHeaders() {
    return defaultHeaders;
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
   * Returns the Authenticator instance currently set on this BaseService instance.
   * @return the Authenticator set on this BaseService
   */
  public Authenticator getAuthenticator() {
    return this.authenticator;
  }

  /**
   * Set the service URL (the base URL for the service instance).
   * @param serviceUrl the new service URL value
   */
  public void setServiceUrl(String serviceUrl) {
    if (CredentialUtils.hasBadStartOrEndChar(serviceUrl)) {
      throw new IllegalArgumentException("The URL shouldn't start or end with curly brackets or quotes. Please "
          + "remove any surrounding {, }, or \" characters.");
    }

    // Remove any potential trailing / character from the input value.
    String newValue = serviceUrl;
    if ((newValue != null) && !newValue.isEmpty()) {
      newValue = newValue.endsWith("/") ? newValue.substring(0, newValue.length() - 1) : newValue;
    }
    this.serviceUrl = newValue;
    LOG.log(Level.FINE, "Set service URL: {0}", this.serviceUrl);
  }

  /**
   * Returns the service URL value associated with this service instance.
   * @return the service URL
   */
  public String getServiceUrl() {
    return this.serviceUrl;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder().append(name).append(" [");
    builder.append("serviceUrl=").append(serviceUrl != null ? serviceUrl : "<null>");
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
      try {
        return converter.convert(response);
      } catch (Throwable t) {
        throw new InvalidServiceResponseException(response, "Error processing the HTTP response", t);
      }
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
        throw new NotAcceptableException(response);
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
   * Defines implementation for modifying and executing service calls.
   *
   * @param <T> the generic type
   */
  class IBMCloudSDKServiceCall<T> implements ServiceCall<T> {
    private Call call;
    private ResponseConverter<T> converter;

    IBMCloudSDKServiceCall(Call call, ResponseConverter<T> converter) {
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
        if (e instanceof SSLHandshakeException) {
          LOG.log(Level.WARNING, ERRORMSG_SSL);
        }
        throw new RuntimeException(e);
      }
    }

    @Override
    public void enqueue(final ServiceCallback<T> callback) {
      call.enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
          if (e instanceof SSLHandshakeException) {
            LOG.log(Level.WARNING, ERRORMSG_SSL);
          }
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
        public com.ibm.cloud.sdk.core.http.Response<T> call() {
          try {
            Response response = call.execute();
            T responseModel = processServiceCall(converter, response);
            return new com.ibm.cloud.sdk.core.http.Response<>(responseModel, response);
          } catch (IOException e) {
            if (e instanceof SSLHandshakeException) {
              LOG.log(Level.WARNING, ERRORMSG_SSL);
            }
            throw new RuntimeException(e);
          }
        }
      });
    }

    @Override
    public void cancel() {
      this.call.cancel();
    }

    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      if (!call.isExecuted()) {
        final Request r = call.request();
        LOG.log(Level.WARNING, "Request {0} {1} has not been sent.  Did you forget to call execute()?",
            new Object[] { r.method(), r.url().toString() });
      }
    }
  }
}
