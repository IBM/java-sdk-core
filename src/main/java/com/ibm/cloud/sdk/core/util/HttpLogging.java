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

package com.ibm.cloud.sdk.core.util;

import java.util.logging.Logger;

import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * HttpLogging logs HTTP request and response data.
 *
 * Instantiates a new HTTP logging. The logging level will be determinate by the {@link Logger} used in this class.
 * Basic HTTP request response will be log if Level is INFO, HEADERS if level is FINE and all the bodies if Level is
 * ALL.
 *
 * @deprecated This class functionality is now handled by {@link com.ibm.cloud.sdk.core.http.HttpConfigOptions} and
 * should no longer be needed.
 */
public class HttpLogging {
  private static final Logger LOG = Logger.getLogger(HttpLogging.class.getName());

  private HttpLogging() { }

  private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
  static {
    if (LOG.isLoggable(java.util.logging.Level.ALL)) {
      loggingInterceptor.setLevel(Level.BODY);
    } else if (LOG.isLoggable(java.util.logging.Level.FINE)) {
      loggingInterceptor.setLevel(Level.HEADERS);
    } else if (LOG.isLoggable(java.util.logging.Level.INFO)) {
      loggingInterceptor.setLevel(Level.BASIC);
    }
  }

  public static HttpLoggingInterceptor getLoggingInterceptor() {
    return loggingInterceptor;
  }
}
