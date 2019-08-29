/**
 * (C) Copyright IBM Corp. 2019.
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

package com.ibm.cloud.sdk.core.security;

import java.util.Map;

import okhttp3.Request.Builder;

/**
 * This class is a placeholder implementation of the Authenticator interface
 * which performs no authentication of outgoing REST API requests.
 */
public class NoAuthAuthenticator implements Authenticator {

  public NoAuthAuthenticator() {
  }

  public NoAuthAuthenticator(Map<String, String> config) {
  }


  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_NOAUTH;
  }

  @Override
  public void authenticate(Builder builder) {
    // do nothing
  }

  @Override
  public void validate() {
  }
}
