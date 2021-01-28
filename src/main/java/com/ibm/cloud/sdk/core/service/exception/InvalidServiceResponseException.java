/**
 * (C) Copyright IBM Corp. 2021.
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

import okhttp3.Response;

/**
 * This exception class represents an invalid response body received from the server.
 */
public class InvalidServiceResponseException extends ServiceResponseException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new exception that indicates a failure while processing the
   * response received from the server for an otherwise successful operation.
   *
   * @param response the HTTP response
   * @param message a message summarizing the error condition
   * @param cause the specific exception that was caught while processing the response
   */
  public InvalidServiceResponseException(Response response, String message, Throwable cause) {
    super(response.code(), response, message, cause);
  }
}
