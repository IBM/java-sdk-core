/**
 * (C) Copyright IBM Corp. 2015, 2019.
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

import com.ibm.cloud.sdk.core.http.HttpStatus;

import okhttp3.Response;

/**
 * 413 Request Entity Too Large (HTTP/1.1 - RFC 2616).
 */
public class RequestTooLargeException extends ServiceResponseException {

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new Request Too Large Exception.
   *
   * @param response the HTTP response
   */
  public RequestTooLargeException(Response response) {
    super(HttpStatus.REQUEST_TOO_LONG, response);
    if (this.getMessage() == null) {
      this.setMessage("Request too large: The request entity is larger than the server is able to process");
    }
  }

}
