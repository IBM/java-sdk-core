/**
 * Copyright 2019 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.security.basicauth;

import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.AuthenticatorConfig;
import com.ibm.cloud.sdk.core.util.CredentialUtils;

/**
 * Options for authenticating via Basic Authentication.
 * This authentication mechanism allows the user to supply a username and password.
 * These values are then used to form an Authorization header which is then
 * used within outgoing REST API requests.
 */
public class BasicAuthConfig implements AuthenticatorConfig {
  private String username;
  private String password;

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_BASIC;
  }

  @Override
  public void validate() {
    if (CredentialUtils.hasBadStartOrEndChar(username) || CredentialUtils.hasBadStartOrEndChar(password)) {
      throw new IllegalArgumentException("The username and password shouldn't start or end with curly brackets or "
          + "quotes. Please remove any surrounding {, }, or \" characters.");
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public static class Builder {
    private String username;
    private String password;

    public BasicAuthConfig build() {
      BasicAuthConfig config = new BasicAuthConfig(this);
      config.validate();
      return config;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }
  }

  private BasicAuthConfig(Builder builder) {
    this.username = builder.username;
    this.password = builder.password;
  }
}
