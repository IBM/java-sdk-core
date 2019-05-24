/*
 * Copyright 2018 IBM Corp. All Rights Reserved.
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
package com.ibm.cloud.sdk.core.service.security;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.security.Authenticator;
import com.ibm.cloud.sdk.security.AuthenticatorConfig;

/**
 * Options for authenticating via IAM (Identity and Access Management).
 * This authentication mechanism allows an IAM access token to be obtained using these
 * configuration scenarios:
 * 1) The user supplies an IAM API key and optional URL.
 * 2) The user supplies a username value equal to "apikey" and a password that contains the IAM API key,
 * along with an optional URL.
 * 3) The user supplies an IAM access token.
 * For #1 and #2, the authenticator will interact with the IAM token server (located at the user-supplied URL, or
 * a default URL if not specified) to obtain the IAM access token.   The Java Core will be responsible for
 * refreshing or re-obtaining the access token when it expires.
 * For #3, the user is responsible for all aspects of obtaining and refreshing the IAM access token.
 *
 * The IAM access token is then specified as a Bearer Token within outgoing REST API requests.
 */
public class IamOptions implements AuthenticatorConfig {
  private String apiKey;
  private String accessToken;
  private String url;
  private String clientId;
  private String clientSecret;
  private boolean disableSSLVerification;

  public IamOptions() {
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_IAM;
  }

  @Override
  public void validate() {
    if (StringUtils.isEmpty(apiKey) && StringUtils.isEmpty(accessToken)) {
      throw new IllegalArgumentException("You must provide either the apiKey or accessToken properties.");
    }

    if (StringUtils.isEmpty(clientId) && StringUtils.isEmpty(clientSecret)) {
      // both empty is ok.
    } else if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
      throw new IllegalArgumentException(
        "You must provide both clientId and clientSecret together or provide neither.");
    }
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getUrl() {
    return url;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public boolean getDisableSSLVerification() {
    return disableSSLVerification;
  }

  public static class Builder {
    private String apiKey;
    private String accessToken;
    private String url;
    private String clientId;
    private String clientSecret;

    public IamOptions build() {
      return new IamOptions(this);
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }
  }

  private IamOptions(Builder builder) {
    this.apiKey = builder.apiKey;
    this.accessToken = builder.accessToken;
    this.url = builder.url;
    this.clientId = builder.clientId;
    this.clientSecret = builder.clientSecret;
  }
}
