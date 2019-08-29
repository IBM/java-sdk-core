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

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;

import java.net.CookieHandler;
import java.util.List;

/**
 * This is an adapter that uses {@link JavaNetCookieJar}.
 *
 */
public final class ServiceCookieJar implements CookieJar {
  private JavaNetCookieJar adapter;

  /**
   * Instantiates a new ServiceCookieJar.
   *
   * @param cookieHandler the cookie handler
   */
  public ServiceCookieJar(CookieHandler cookieHandler) {
    this.adapter = new JavaNetCookieJar(cookieHandler);
  }

  /*
   * (non-Javadoc)
   *
   * @see okhttp3.CookieJar#saveFromResponse(okhttp3.HttpUrl, java.util.List)
   */
  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    this.adapter.saveFromResponse(url, cookies);
  }


  /*
   * (non-Javadoc)
   *
   * @see okhttp3.CookieJar#loadForRequest(okhttp3.HttpUrl)
   */
  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    return this.adapter.loadForRequest(url);
  }

}
