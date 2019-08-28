/**
 * Copyright 2017 IBM Corp. All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.service.BaseService;

/**
 * CredentialUtils retrieves service credentials from the environment.
 */
public final class CredentialUtils {
  private static final Logger log = Logger.getLogger(CredentialUtils.class.getName());

  public static final String PLAN_STANDARD = "standard";

  private static final String DEFAULT_CREDENTIAL_FILE_NAME = "ibm-credentials.env";

  private static final String VCAP_SERVICES = "VCAP_SERVICES";

  private static final String CREDENTIALS = "credentials";
  private static final String PLAN = "plan";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String URL = "url";
  private static final String IAM_APIKEY = "iam_apikey";
  // this value was used previously for IAM API keys as well
  private static final String APIKEY = "apikey";
  private static final String IAM_URL = "iam_url";

  private CredentialUtils() {
    // This is a utility class - no instantiation allowed.
  }

  /**
   * Returns true if the supplied value begins or ends with curly brackets or quotation marks. Returns false for null
   * inputs.
   *
   * @param credentialValue the credential value to check
   * @return true if the value starts or ends with these characters and is therefore invalid
   */
  public static boolean hasBadStartOrEndChar(String credentialValue) {
    return credentialValue != null
        && (credentialValue.startsWith("{")
        || credentialValue.startsWith("\"")
        || credentialValue.endsWith("}")
        || credentialValue.endsWith("\""));
  }

  // VCAP-related methods

  /**
   * Gets the <b>VCAP_SERVICES</b> environment variable and return it as a {@link JsonObject}.
   *
   * @return the VCAP_SERVICES as a {@link JsonObject}.
   */
  private static JsonObject getVcapServices() {
    final String envServices = EnvironmentUtils.getenv(VCAP_SERVICES);
    if (envServices == null) {
      return null;
    }

    JsonObject vcapServices = null;

    try {
      final JsonParser parser = new JsonParser();
      vcapServices = (JsonObject) parser.parse(envServices);
    } catch (final JsonSyntaxException e) {
      log.log(Level.INFO, "Error parsing VCAP_SERVICES", e);
    }
    return vcapServices;
  }

  /**
   * A helper method to retrieve the appropriate 'credentials' JSON property value from the VCAP_SERVICES.
   *
   * @param vcapServices JSON object representing the VCAP_SERVICES
   * @param serviceName the name of the service whose credentials are sought
   * @param plan the name of the plan for which the credentials are sought, e.g. 'standard', 'beta' etc, may be null
   * @return the first set of credentials that match the search criteria, service name and plan. May return null
   */
  private static JsonObject getCredentialsObject(JsonObject vcapServices, String serviceName, String plan) {
    for (final Entry<String, JsonElement> entry : vcapServices.entrySet()) {
      final String key = entry.getKey();
      if (key.startsWith(serviceName)) {
        final JsonArray servInstances = vcapServices.getAsJsonArray(key);
        for (final JsonElement instance : servInstances) {
          final JsonObject service = instance.getAsJsonObject();
          final String instancePlan = service.get(PLAN).getAsString();
          if ((plan == null) || plan.equalsIgnoreCase(instancePlan)) {
            return instance.getAsJsonObject().getAsJsonObject(CREDENTIALS);
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the property named 'key' associated with the specified service.
   * @param serviceName the name of the service
   * @param key the name of the property to retrieve
   * @return the value of the specified property
   */
  public static String getVcapValue(String serviceName, String key) {
    return getVcapValue(serviceName, key, null);
  }

  /**
   * Returns the value associated with the provided key from the VCAP_SERVICES, or null if it doesn't exist. In the
   * case of the API URL, if VCAP_SERVICES aren't present, this method will also search in JNDI.
   *
   * @param serviceName the service name
   * @param key the key whose value should be returned
   * @param plan the plan name
   * @return the value of the provided key
   */
  public static String getVcapValue(String serviceName, String key, String plan) {
    if ((serviceName == null) || serviceName.isEmpty()) {
      return null;
    }

    final JsonObject services = getVcapServices();
    if (services == null) {
      return null;
    }

    JsonObject jsonCredentials = getCredentialsObject(services, serviceName, plan);
    if (jsonCredentials != null) {
      if (jsonCredentials.has(key)) {
        return jsonCredentials.get(key).getAsString();
      }
    }
    return null;
  }

  // Credential file-related methods

  /**
   * Creates a list of files to check for credentials. The file locations are:
   * * Location provided by user-specified IBM_CREDENTIALS_FILE environment variable
   * * System home directory (Unix)
   * * System home directory (Windows)
   * * Top-level directory of the project this code is being called in
   *
   * @return list of credential files to check
   */
  private static List<File> getFilesToCheck() {
    List<File> files = new ArrayList<>();

    String userSpecifiedPath = EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE");
    String unixHomeDirectory = EnvironmentUtils.getenv("HOME");
    String windowsFirstHomeDirectory = EnvironmentUtils.getenv("HOMEDRIVE") + EnvironmentUtils.getenv("HOMEPATH");
    String windowsSecondHomeDirectory = EnvironmentUtils.getenv("USERPROFILE");
    String projectDirectory = System.getProperty("user.dir");

    if (StringUtils.isNotEmpty(userSpecifiedPath)) {
      files.add(new File(userSpecifiedPath));
    }

    if (StringUtils.isNotEmpty(unixHomeDirectory)) {
      files.add(new File(String.format("%s/%s", unixHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    }

    if (StringUtils.isNotEmpty(windowsFirstHomeDirectory) && !"nullnull".equals(windowsFirstHomeDirectory)) {
      files.add(new File(String.format("%s/%s", windowsFirstHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    }

    if (StringUtils.isNotEmpty(windowsSecondHomeDirectory)) {
      files.add(new File(String.format("%s/%s", windowsSecondHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    }

    if (StringUtils.isNotEmpty(projectDirectory)) {
      files.add(new File(String.format("%s/%s", projectDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    }

    return files;
  }

  /**
   * Looks through the provided list of files to search for credentials, stopping at the first existing file.
   *
   * @return list of lines in the credential file, or null if no file is found
   */
  private static List<String> getFirstExistingFileContents(List<File> files) {
    List<String> credentialFileContents = null;

    try {
      for (File file : files) {
        if (file.isFile()) {
          credentialFileContents = IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8);
          break;
        }
      }
    } catch (IOException e) {
      log.severe("There was a problem trying to read the credential file: " + e);
    }

    return credentialFileContents;
  }

  /**
   * Returns a Map containing properties found within the credential file that are associated with the
   * specified cloud service.
   * @param serviceName the name of the cloud service whose properties should be loaded
   * @return a Map containing the properties
   */
  public static Map<String, String> getFileCredentialsAsMap(String serviceName) {
    List<File> files = getFilesToCheck();
    List<String> contents = getFirstExistingFileContents(files);
    if (contents != null && !contents.isEmpty()) {
      return parseCredentials(serviceName, contents);
    }

    return Collections.emptyMap();
  }

  /**
   * Returns a Map containing properties found within the process' environment that are associated with the
   * specified cloud service.
   * @param serviceName the name of the cloud service whose properties should be retrieved
   * @return a Map containing the properties
   */
  public static Map<String, String> getEnvCredentialsAsMap(String serviceName) {
    // Retrieve the Map of environment variables from the current process.
    Map<String, String> env = EnvironmentUtils.getenv();

    // Extract the properties related to the specified service and populate the result Map.
    if (env != null && !env.isEmpty()) {
      Map<String, String> props = new HashMap<>();
      serviceName = serviceName.toUpperCase().replaceAll("-", "_") + "_";
      for (Map.Entry<String, String> entry : env.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        if (key.startsWith(serviceName)) {
          String credentialName = key.substring(serviceName.length());
          if (StringUtils.isNotEmpty(credentialName) && StringUtils.isNotEmpty(value)) {
            props.put(credentialName, value);
          }
        }
      }
      return props;
    }

    return Collections.emptyMap();
  }

  /**
   * Returns a Map containing properties found within the VCAP_SERVICES environment variable that are associated
   * with the specified cloud service.
   * @param serviceName the name of the cloud service whose properties should be retrieved
   * @return a Map containing the properties
   */
  public static Map<String, String> getVcapCredentialsAsMap(String serviceName) {
    Map<String, String> props = new HashMap<>();
    addToMap(props, Authenticator.PROPNAME_USERNAME, getVcapValue(serviceName, USERNAME));
    addToMap(props, Authenticator.PROPNAME_PASSWORD, getVcapValue(serviceName, PASSWORD));
    addToMap(props, BaseService.PROPNAME_URL, getVcapValue(serviceName, URL));
    addToMap(props, Authenticator.PROPNAME_URL, getVcapValue(serviceName, IAM_URL));

    // For the IAM apikey, the "apikey" property has higher precedence than "iam_apikey".
    addToMap(props, Authenticator.PROPNAME_APIKEY, getVcapValue(serviceName, IAM_APIKEY));
    addToMap(props, Authenticator.PROPNAME_APIKEY, getVcapValue(serviceName, APIKEY));

    // Try to guess at the auth type based on the properties found.
    if (StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_APIKEY))) {
      addToMap(props, Authenticator.PROPNAME_AUTH_TYPE, Authenticator.AUTHTYPE_IAM);
    } else if (StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_USERNAME))
        || StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_PASSWORD))) {
      addToMap(props, Authenticator.PROPNAME_AUTH_TYPE, Authenticator.AUTHTYPE_BASIC);
    }

    return props;
  }

  /**
   * This function forms a wrapper around the "getFileCredentialsAsMap", "getEnvCredentialsAsMap", and
   * "getVcapCredentialsAsMap" methods and provides a convenient way to retrieve the configuration
   * properties for the specified service from any of the three config sources.
   * The properties are retrieved from one of the following sources (in precendence order):
   * 1) Credential file
   * 2) Environment variables
   * 3) VCAP_SERVICES
   * @param serviceName the name of the service
   * @return a Map of properties associated with the service
   */
  public static Map<String, String> getServiceProperties(String serviceName) {
    Map<String, String> props = getFileCredentialsAsMap(serviceName);
    if (props.isEmpty()) {
      props = getEnvCredentialsAsMap(serviceName);
    }
    if (props.isEmpty()) {
      props = getVcapCredentialsAsMap(serviceName);
    }
    return props;
  }

  /**
   * Adds the specified key/value pair to the map if the value is not null or "".
   * @param map the map
   * @param key the key
   * @param value the value
   */
  private static void addToMap(Map<String, String> map, String key, String value) {
    if (StringUtils.isNotEmpty(value)) {
      map.put(key, value);
    }
  }

  /**
   * Parses each of the entries in "contents" that are related to the specified cloud service.
   * @param serviceName the name of the service whose properties will be returned
   * @param contents a list of strings representing the contents of a credential file
   * @return a Map containing the properties related to the specified cloud service
   */
  protected static Map<String, String> parseCredentials(String serviceName, List<String> contents) {
    Map<String, String> props = new HashMap<>();

    serviceName = serviceName.toUpperCase().replaceAll("-", "_") + "_";

    // Within "contents", we're looking for lines of the form:
    //    <serviceName>_<credentialName>=<value>
    //    Example:  ASSISTANT_APIKEY=myapikey
    // Each such line will be parsed into <credentialName> and <value>,
    // and added to the result Map.
    for (String line : contents) {
      // Skip comment lines and empty lines.
      if (line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }

      String[] lineTokens = line.split("=");
      if (lineTokens.length != 2) {
        continue;
      }

      String key = lineTokens[0];
      String value = lineTokens[1];

      if (key.startsWith(serviceName)) {
        String credentialName = key.substring(serviceName.length());
        if (StringUtils.isNotEmpty(credentialName) && StringUtils.isNotEmpty(value)) {
          props.put(credentialName, value);
        }
      }
    }

    return props;
  }
}
