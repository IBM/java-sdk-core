/**
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
package com.ibm.cloud.sdk.core.service.exception;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.util.GsonSingleton;
import com.ibm.cloud.sdk.core.util.ResponseUtils;
import okhttp3.Response;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Generic Service Response Exception.
 */
public class ServiceResponseException extends RuntimeException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** Potential error message keys */
  private static final String MESSAGE_ERROR = "error";
  private static final String MESSAGE_ERROR_2 = "error_message";
  private static final String MESSAGE_ERROR_3 = "message";

  private static final Type debuggingInfoType = new TypeToken<Map<String, String>>() {}.getType();

  /** The status code. */
  private final int statusCode;

  private String message;
  private Map<String, String> debuggingInfo;

  /**
   * Instantiates a new Service Response Exception.
   *
   * @param statusCode the status code
   * @param response the HTTP response
   */
  public ServiceResponseException(int statusCode, Response response) {
    super();
    this.statusCode = statusCode;

    String responseString = ResponseUtils.getString(response);
    try {
      final JsonObject jsonObject = ResponseUtils.getJsonObject(responseString);
      if (jsonObject.has(MESSAGE_ERROR)) {
        this.message = jsonObject.remove(MESSAGE_ERROR).getAsString();
      } else if (jsonObject.has(MESSAGE_ERROR_2)) {
        this.message = jsonObject.remove(MESSAGE_ERROR_2).getAsString();
      } else if (jsonObject.has(MESSAGE_ERROR_3)) {
        this.message = jsonObject.remove(MESSAGE_ERROR_3).getAsString();
      }
      this.debuggingInfo = GsonSingleton.getGson().fromJson(jsonObject, debuggingInfoType);
    } catch (final Exception e) {
      // Ignore any kind of exception parsing the json and use fallback String version of response
      this.message = responseString;
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
  public String getMessage() {
    return message;
  }

  /**
   * Gets the response information other than the error message.
   *
   * @return the response information other than the error message
   */
  public Map<String, String> getDebuggingInfo() {
    return debuggingInfo;
  }
}
