/**
 * (C) Copyright IBM Corp. 2022.  All Rights Reserved.
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

import com.ibm.cloud.sdk.core.security.Authenticator;

/**
 * IRetryStrategy is an interface that is implemented by retry interceptor factories.
 * This interface is used by the java core to create a retry interceptor implementation when
 * automatic retries are enabled.
 * The java core defines a default implementation of this interface in the
 * DefaultRetryStrategy class.
 * Users can implement their own factory in order to supply their own retry interceptor
 * implementation, perhaps to customize the criteria under which failed requests will be retried.
 */
public interface IRetryStrategy {

  /**
   * Return an implementation of the {@link IRetryInterceptor} interface
   * that is capable of retrying failed requests.
   *
   * @param maxRetries the maximum number of retries to be attempted by the retry interceptor
   * @param maxRetryInterval the maximum interval (in seconds)
   * @param authenticator the Authenticator instance to be used to authenticate retried requests
   * @return an okhttp3.Interceptor instance
   */
  IRetryInterceptor createRetryInterceptor(int maxRetries, int maxRetryInterval, Authenticator authenticator);
}
