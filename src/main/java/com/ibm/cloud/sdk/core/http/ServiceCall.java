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

package com.ibm.cloud.sdk.core.http;

import io.reactivex.Single;

/**
 * Service Call.
 *
 * @param <T> the generic type
 */
public interface ServiceCall<T> {

  /**
   * Add a header to the request before executing.
   *
   * @param name the name of the header
   * @param value the value of the header
   * @return the ServiceCall with updated headers
   */
  ServiceCall<T> addHeader(String name, String value);

  /**
   * Synchronous request.
   *
   * @return a Response object with the generic response model and various HTTP information fields
   * @throws RuntimeException the exception from HTTP request
   */
  Response<T> execute() throws RuntimeException;

  /**
   * Asynchronous request with added HTTP information. In this case, you receive a callback when the data has been
   * received.
   *
   * @param callback the callback
   */
  void enqueue(ServiceCallback<T> callback);

  /**
   * Reactive request using the RxJava 2 library. See https://github.com/ReactiveX/RxJava. In addition, the wrapped
   * service call will contain added HTTP information.
   *
   * @return a Single object containing the service call to be observed/subscribed to
   */
  Single<Response<T>> reactiveRequest();

  /**
   * Cancel the current request if possible.
   */
  void cancel();
}
