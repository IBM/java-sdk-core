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

import java.util.Map;

/**
 * This is a utility class that contains methods related to environment variables.
 */
public class EnvironmentUtils {

  // hide default ctor since this is a utility class.
  protected EnvironmentUtils() {
  }

  /**
   * Wrapper around System.getenv(String) to allow us to mock it during testing.
   * @param varname the name of the environment variable to retrieve
   * @return the value of the specified environment variable
   */
  public static String getenv(String varname) {
    return System.getenv(varname);
  }

  /**
   * Wrapper around System.getenv()) to allow us to mock it during testing.
   * @return a Map containing the current process' environment variables
   */
  public static Map<String, String> getenv() {
    return System.getenv();
  }
}
