/**
 * (C) Copyright IBM Corp. 2019, 2021.
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
  public static final String ERRORMSG_AUTHTYPE_UNKNOWN = "Unrecognized authentication type: %s";

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
    Map<String, String> authProps = CredentialUtils.getServiceProperties(serviceName);

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

    // If auth type was not specified, we'll use "iam" as the default if the "apikey" property
    // is present, otherwise we'll use "container" as the default.
    String authType = props.get(Authenticator.PROPNAME_AUTH_TYPE);

    // Check for the alternate "AUTHTYPE" property.
    if (StringUtils.isEmpty(authType)) {
      authType = props.get("AUTHTYPE");
    }

    // Determine the default auth type if it wasn't configured by the user.
    if (StringUtils.isEmpty(authType)) {
      if (props.get(Authenticator.PROPNAME_APIKEY) != null || props.get("IAM_APIKEY") != null) {
        authType = Authenticator.AUTHTYPE_IAM;
      } else {
        authType = Authenticator.AUTHTYPE_CONTAINER;
      }
    }

    // Create the appropriate authenticator based on the auth type.
    if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_BASIC)) {
      authenticator = BasicAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_BEARER_TOKEN)) {
      authenticator = BearerTokenAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_CP4D)) {
      authenticator = CloudPakForDataAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_CP4D_SERVICE)) {
      authenticator = CloudPakForDataServiceAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_CP4D_SERVICE_INSTANCE)) {
      authenticator = CloudPakForDataServiceInstanceAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_IAM)) {
      authenticator = IamAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_CONTAINER)) {
      authenticator = ContainerAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_VPC)) {
      authenticator = VpcInstanceAuthenticator.fromConfiguration(props);
    } else if (authType.equalsIgnoreCase(Authenticator.AUTHTYPE_NOAUTH)) {
      authenticator = new NoAuthAuthenticator(props);
    } else {
      throw new IllegalArgumentException(String.format(ERRORMSG_AUTHTYPE_UNKNOWN, authType));
    }

    return authenticator;
  }
}
