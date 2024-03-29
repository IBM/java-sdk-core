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

package com.ibm.cloud.sdk.core.service.exception;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.http.Headers;
import com.ibm.cloud.sdk.core.util.GsonSingleton;
import com.ibm.cloud.sdk.core.util.ResponseUtils;
import okhttp3.Response;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Generic Service Response Exception.
 */
public class ServiceResponseException extends RuntimeException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** Potential error message keys. */
  private static final String ERRORS_KEY = "errors";
  private static final String MESSAGE_STRING = "message";
  private static final String ERROR_STRING = "error";
  private static final String ERROR_MESSAGE = "errorMessage";

  private static final Type debuggingInfoType = new TypeToken<Map<String, Object>>() { }.getType();

  /** The status code. */
  private int statusCode;

  private String message;
  private Headers headers;
  private Map<String, Object> debuggingInfo;
  private String responseBody;

  /**
   * Instantiates a new Service Response Exception for a response that resulted
   * in a bad status code.
   *
   * @param statusCode the status code
   * @param response the HTTP response
   */
  public ServiceResponseException(int statusCode, Response response) {
    super();
    init(statusCode, response, response.headers());
  }

  /**
   * Instantiates a new Service Response Exception when we have a specific exception to wrap.
   *
   * @param statusCode the status code
   * @param response the HTTP response
   * @param message a message to include in this exception
   * @param cause the specific exception to wrap inside this exception
   */
  public ServiceResponseException(int statusCode, Response response, String message, Throwable cause) {
    super(cause);
    init(statusCode, null, response.headers());
    setMessage(message);
  }

  private void init(int statusCode, Response response, okhttp3.Headers headers) {
    this.statusCode = statusCode;
    this.headers = new Headers(headers);

    if (response != null) {
      String responseString = ResponseUtils.getString(response);
      if (StringUtils.isNotEmpty(responseString)) {
        this.responseBody = responseString;
        try {
          final JsonObject jsonObject = ResponseUtils.getJsonObject(responseString);
          if (jsonObject.has(ERRORS_KEY)) {
            this.message = jsonObject.remove(ERRORS_KEY).getAsJsonArray().get(0).getAsJsonObject()
                .remove(MESSAGE_STRING).getAsString();
          } else if (jsonObject.has(ERROR_STRING)) {
            this.message = jsonObject.remove(ERROR_STRING).getAsString();
          } else if (jsonObject.has(MESSAGE_STRING)) {
            this.message = jsonObject.remove(MESSAGE_STRING).getAsString();
          } else if (jsonObject.has(ERROR_MESSAGE)) {
            this.message = jsonObject.remove(ERROR_MESSAGE).getAsString();
          }
          this.debuggingInfo = GsonSingleton.getGson().fromJson(jsonObject, debuggingInfoType);
        } catch (final Exception e) {
          // Ignore any kind of exception parsing the json and use fallback String version
          // of response
          this.message = responseString;
        }
      }
    }
  }

  /**
   * Gets the HTTP status code.
   *
   * @return the http status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  @Override
  public String getMessage() {
    return message;
  }

  /**
   * Sets the error message.
   *
   * @param message the error message
   */
  protected void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the headers.
   *
   * @return the headers
   */
  public Headers getHeaders() {
    return headers;
  }

  /**
   * Gets the response information other than the error message.
   *
   * @return the response information other than the error message
   */
  public Map<String, Object> getDebuggingInfo() {
    return debuggingInfo;
  }

  /**
   * Gets the error response body as a string.
   *
   * @return the response body as a string
   */
  public String getResponseBody() {
    return responseBody;
  }
}
