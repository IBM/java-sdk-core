/*
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
package com.ibm.cloud.sdk.security.icp4d;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.security.Authenticator;
import com.ibm.cloud.sdk.security.AuthenticatorConfig;

/**
 * Options for authenticating via the ICP4D (IBM Cloud Private for Data) token service.
 * This authentication mechanism allows an ICP4D access token to be obtained using a
 * user-supplied username, password and URL.
 * The ICP4D access token is then specified as a Bearer Token within outgoing REST API requests.
 */
public class ICP4DConfig implements AuthenticatorConfig {
  private String url;
  private String username;
  private String password;
  private String userManagedAccessToken;
  private boolean disableSSLVerification;

  public ICP4DConfig() {
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_ICP4D;
  }

  @Override
  public void validate() {
    if (StringUtils.isEmpty(url)) {
      throw new IllegalArgumentException("You must provide a URL.");
    }

    // If the user specifies their own access token, then username/password are not required.
    if (StringUtils.isNotEmpty(userManagedAccessToken)) {
      return;
    }

    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      throw new IllegalArgumentException(
        "You must provide both username and password values.");
    }
  }

  public String getUrl() {
    return this.url;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getUserManagedAccessToken() {
    return this.userManagedAccessToken;
  }

  public boolean isDisableSSLVerification() {
    return this.disableSSLVerification;
  }

  public static class Builder {
    private String url;
    private String username;
    private String password;
    private String userManagedAccessToken;
    private boolean disableSSLVerification;

    public ICP4DConfig build() {
      ICP4DConfig config = new ICP4DConfig(this);
      config.validate();
      return config;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder userManagedAccessToken(String userManagedAccessToken) {
      this.userManagedAccessToken = userManagedAccessToken;
      return this;
    }

    public Builder disableSSLVerification(boolean disableSSLVerification) {
      this.disableSSLVerification = disableSSLVerification;
      return this;
    }
  }

  private ICP4DConfig(Builder builder) {
    this.url = builder.url;
    this.username = builder.username;
    this.password = builder.password;
    this.userManagedAccessToken = builder.userManagedAccessToken;
    this.disableSSLVerification = builder.disableSSLVerification;
  }
}
