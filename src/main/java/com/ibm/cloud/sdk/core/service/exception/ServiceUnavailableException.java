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
 * 503 Service Unavailable (HTTP/1.0 - RFC 1945).
 */
public class ServiceUnavailableException extends ServiceResponseException {

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new Service Unavailable Exception.
   *
   * @param response the HTTP response
   */
  public ServiceUnavailableException(Response response) {
    super(HttpStatus.SERVICE_UNAVAILABLE, response);
    if (this.getMessage() == null) {
      this.setMessage("Service unavailable");
    }
  }

}
