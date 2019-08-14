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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.util.CredentialUtils;

/**
 * This class serves as an Authenticator factory.
 * It will detect and use various configuration sources in order to produce an Authenticator instance.
 */
public class ConfigBasedAuthenticatorFactory {

  // The default ctor is hidden since this is a utility class.
  protected ConfigBasedAuthenticatorFactory() {
  }

  /**
   * Retrieves authentication configuration information for the specified cloud service,
   * and returns an Authenticator instance, or null if the configuration information could not be found.
   * @param serviceName the name of the cloud service whose authentication information should be retrieved
   * @return an Authenticator that reflects the properties that were found in the various config sources
   */
  public static Authenticator getAuthenticator(String serviceName) {
    Authenticator authenticator = null;

    // Gather authentication-related properties from all the supported config sources:
    // - 1) Credential file
    // - 2) Environment variables
    // - 3) VCAP_SERVICES env variable
    Map<String, String> authProps;

    // First check to see if this service has any properties defined in a credential file.
    authProps = CredentialUtils.getFileCredentialsAsMap(serviceName);

    // If we didn't find any properties so far, then try the environment.
    if (authProps.isEmpty()) {
      authProps = CredentialUtils.getEnvCredentialsAsMap(serviceName);
    }

    // If we didn't find any properties so far, then try VCAP_SERVICES
    if (authProps.isEmpty()) {
      authProps = CredentialUtils.getVcapCredentialsAsMap(serviceName);
    }

    // Now create an authenticator from the map.
    if (!authProps.isEmpty()) {
      authenticator = createAuthenticator(authProps);
    }

    return authenticator;
  }

  /**
   * Instantiates an Authenticator that reflects the properties contains in the specified Map.
   * @param props a Map containing configuration properties
   * @return an Authenticator instance
   */
  private static Authenticator createAuthenticator(Map<String, String> props) {
    Authenticator authenticator = null;

    // If auth type was not specified, we'll use "iam" as the default.
    String authType = props.get(Authenticator.PROPNAME_AUTH_TYPE);
    if (StringUtils.isEmpty(authType)) {
      authType = Authenticator.AUTHTYPE_IAM;
    }

    switch (authType) {
      case Authenticator.AUTHTYPE_NOAUTH:
        authenticator = new NoauthAuthenticator(props);
        break;

      case Authenticator.AUTHTYPE_BASIC:
        authenticator = new BasicAuthenticator(props);
        break;

      case Authenticator.AUTHTYPE_IAM:
        authenticator = new IamAuthenticator(props);
        break;

      case Authenticator.AUTHTYPE_CP4D:
        authenticator = new CloudPakForDataAuthenticator(props);
        break;

      case Authenticator.AUTHTYPE_BEARER_TOKEN:
        authenticator = new BearerTokenAuthenticator(props);
        break;
      default:
        break;
    }

    return authenticator;
  }
}
