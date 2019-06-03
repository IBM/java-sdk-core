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
package com.ibm.cloud.sdk.core.security;

import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthConfig;
import com.ibm.cloud.sdk.core.security.basicauth.BasicAuthenticator;
import com.ibm.cloud.sdk.core.security.icp4d.ICP4DAuthenticator;
import com.ibm.cloud.sdk.core.security.icp4d.ICP4DConfig;
import com.ibm.cloud.sdk.core.security.noauth.NoauthAuthenticator;
import com.ibm.cloud.sdk.core.security.noauth.NoauthConfig;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.cloud.sdk.core.service.security.IamTokenManager;

/**
 * This class serves as a factory for creating an Authenticator instance from an AuthenticatorConfig.
 */
public class AuthenticatorFactory {

  /**
   * Hide the default ctor since this is a utility class.
   */
  private AuthenticatorFactory() {
  }

  /**
   * Instantiates the appropriate Authenticator implementation for the specified config instance.
   * @param config the AuthenticatorConfig instance which contains the configuration information for the desired
   * Authenticator
   * @return an Authenticator instance
   * @throws IllegalArgumentException if validation of 'config' failed
   */
  public static Authenticator getAuthenticator(AuthenticatorConfig config) throws IllegalArgumentException {

    // Validate the configuration passed in.
    config.validate();

    // Create an authenticator from the config.
    // TODO in the future, we could improve this factory so that it "discovers" Authenticator
    // implementations (perhaps via the ServiceLoader mechanism), rather than explicitly checking
    // for specific ones here.
    switch (config.authenticationType()) {
      case Authenticator.AUTHTYPE_IAM:
        return new IamTokenManager((IamOptions) config);
      case Authenticator.AUTHTYPE_ICP4D:
        return new ICP4DAuthenticator((ICP4DConfig) config);
      case Authenticator.AUTHTYPE_BASIC:
        return new BasicAuthenticator((BasicAuthConfig) config);
      case Authenticator.AUTHTYPE_NOAUTH:
        return new NoauthAuthenticator(((NoauthConfig) config));
      default:
        throw new IllegalArgumentException("Unrecognized AuthenticatorConfig type: " + config.getClass().getName());
    }
  }
}
