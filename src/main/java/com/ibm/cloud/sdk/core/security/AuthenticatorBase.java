/**
 * (C) Copyright IBM Corp. 2019, 2023.
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

import java.util.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * This is a common base class used with the various Authenticator implementations.
 */
public class AuthenticatorBase {

  /**
   * Common error messages.
   */
  public static final String ERRORMSG_PROP_MISSING = "The %s property is required but was not specified.";
  public static final String ERRORMSG_PROP_INVALID =
      "The %s property is invalid. Please remove any surrounding {, }, or \" characters.";
  public static final String ERRORMSG_REQ_FAILED = "Error while fetching access token from token service: ";
  public static final String ERRORMSG_EXCLUSIVE_PROP_ERROR = "Exactly one of %s or %s must be specified.";
  public static final String ERRORMSG_ATLEAST_ONE_PROP_ERROR = "At least one of %s or %s must be specified.";
  public static final String ERRORMSG_ATMOST_ONE_PROP_ERROR = "At most one of %s or %s may be specified.";
  public static final String ERRORMSG_PROP_INVALID_INTEGER_VALUE =
          "The %s property must be a valid integer but was %s.";
  /**
   * Returns a "Basic" Authorization header value for the specified username and password.
   * @param username the username
   * @param password the password
   * @return the Authorization header value in the form "Basic &lt;encoded username and password&gt;"
   */
  public static String constructBasicAuthHeader(String username, String password) {
    if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
      return "Basic " + Base64.getEncoder().encodeToString(((username + ":" + password).getBytes()));
    }
    return null;
  }

  /**
   * Returns a "Bearer" Authorization header value for the specified bearer token.
   * @param bearerToken the token value to be included in the header
   * @return the Authorization header value in the form "Bearer &lt;bearerToken&gt;"
   */
  public static String constructBearerTokenAuthHeader(String bearerToken) {
    if (StringUtils.isNotEmpty(bearerToken)) {
      return "Bearer " + bearerToken;
    }
    return null;
  }
}
