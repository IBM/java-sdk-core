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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

  private CredentialUtils() {
    // This is a utility class - no instantiation allowed.
  }

  /**
   * This function will retrieve configuration properties for the specified service from the following
   * external config sources (in priority order):
   * 1) Credential file
   * 2) Environment variables
   * 3) VCAP_SERVICES
   * The properties are returned in a Map.
   *
   * @param serviceName the name of the service. When searching for service-related properties in the
   * credential file and environment variable config sources, this service name is transformed by
   * folding it to upper case and replacing "-" with "_". (e.g. "my-service" yields "MY_SERVICE").
   * When searching for the service within the VCAP_SERVICES setting, no transformation of the service name
   * is performed.
   *
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
   * This class is used to unmarshal the contents of the "credentials" field within
   * a vcap service entry.
   */
  public static class VcapCredentials {
    public String url;
    public String username;
    public String password;
    public String apikey;
    @SerializedName("iam_apikey")
    public String iamApiKey;
    @SerializedName("iam_url")
    public String iamUrl;
    @SerializedName("iam_apikey_description")
    public String iamApikeyDescription;
    @SerializedName("iam_apikey_name")
    public String iamApikeyName;
    @SerializedName("iam_role_crn")
    public String iamRoleCrn;
    @SerializedName("iam_serviceid_crn")
    public String iamServiceCrn;
  }

  /**
   * This class is used to unmarshal an item in the list of services belonging to a particular service key.
   */
  public static class VcapService {
    public String name;
    public String label;
    public String plan;
    public List<String> tags;
    public VcapCredentials credentials;
  }

  /**
   * Retrieves the VCAP_SERVICES environment variable and unmarshals it.
   * @return a map containing the unmarshalled VCAP_SERVICES value
   */
  public static Map<String, List<VcapService>> getVcapServicesObj() {
    Map<String, List<VcapService>> result = null;

    // Retrieve the environment variable's value.
    String vcapValue = EnvironmentUtils.getenv(VCAP_SERVICES);
    if (StringUtils.isNotEmpty(vcapValue)) {
      Gson gson = GsonSingleton.getGson();
      // Parse it into a map of VcapService lists keyed by service name.
      Type typeToken = new TypeToken<Map<String, List<VcapService>>>() { }.getType();
      try {
        result = gson.fromJson(vcapValue, typeToken);
      } catch (Throwable t) {
        log.log(Level.WARNING, "Error parsing VCAP_SERVICES", t);
      }
    }
    return result;
  }

   /**
   * Returns an appropriate "service" list item for the specific 'serviceName' value.
   *
   * @param serviceName the name used to locate the appropriate entry within the VCAP_SERVICES structure.
   * This value will be used in two ways:
   *
   * First, a search will be done to find a service entry whose 'name' field matches the 'vcapName'
   * parameter value.
   * Second, if no entry with a matching 'name' field was found, then the first service entry within the
   * services list whose key matches 'serviceName' will be returned.
   *
   *
   * @return a VcapService instance if found, or null
   */
  protected static VcapService getVcapServiceEntry(String serviceName) {
    // check if param is defined
    if (serviceName == null || serviceName.isEmpty()) {
      return null;
    }
    // Retrieve the VCAP_SERVICES environment variable and unmarshal into a Map.
    Map<String, List<VcapService>> vcapObj = getVcapServicesObj();
    if (vcapObj != null) {
      // search the inner entries of the vcap for the matching serviceName
      for (List<VcapService> serviceList : vcapObj.values()) {
        for (VcapService service : serviceList) {
          if (serviceName.equals(service.name)) {
            return service;
          }
        }
      }
      // Second, try to find a service list with the specified key.
      List<VcapService> services = vcapObj.get(serviceName);
      if (services != null && !services.isEmpty()) {
        return services.get(0);
      }
    }
    return null;
  }

  // Credential file-related methods

  /**
   * Creates a list of files to check for credentials. The file locations are:
   * * Location provided by user-specified IBM_CREDENTIALS_FILE environment variable
   * * Current working directory this code is being called in
   * * System home directory (Unix)
   * * System home directory (Windows)
   *
   * @return list of credential files to check
   */
  private static List<File> getFilesToCheck() {
    List<File> files = new ArrayList<>();

    String userSpecifiedPath = EnvironmentUtils.getenv("IBM_CREDENTIALS_FILE");
    String currentWorkingDirectory = System.getProperty("user.dir");
    String unixHomeDirectory = EnvironmentUtils.getenv("HOME");
    String windowsFirstHomeDirectory = EnvironmentUtils.getenv("HOMEDRIVE") + EnvironmentUtils.getenv("HOMEPATH");
    String windowsSecondHomeDirectory = EnvironmentUtils.getenv("USERPROFILE");

    if (StringUtils.isNotEmpty(userSpecifiedPath)) {
      files.add(new File(userSpecifiedPath));
    }

    if (StringUtils.isNotEmpty(currentWorkingDirectory)) {
      files.add(new File(String.format("%s/%s", currentWorkingDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
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
  static Map<String, String> getFileCredentialsAsMap(String serviceName) {
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
  static Map<String, String> getEnvCredentialsAsMap(String serviceName) {
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
  static Map<String, String> getVcapCredentialsAsMap(String serviceName) {
    Map<String, String> props = new HashMap<>();
    // Retrieve the vcap service entry for the specific key and name, then copy its values to the map.
    VcapService vcapService = getVcapServiceEntry(serviceName);

    if (vcapService != null && vcapService.credentials != null) {
      addToMap(props, Authenticator.PROPNAME_USERNAME, vcapService.credentials.username);
      addToMap(props, Authenticator.PROPNAME_PASSWORD, vcapService.credentials.password);
      addToMap(props, BaseService.PROPNAME_URL, vcapService.credentials.url);
      addToMap(props, Authenticator.PROPNAME_URL, vcapService.credentials.iamUrl);
      // For the IAM apikey, the "apikey" property has higher precedence than "iam_apikey".
      addToMap(props, Authenticator.PROPNAME_APIKEY, vcapService.credentials.iamApiKey);
      addToMap(props, Authenticator.PROPNAME_APIKEY, vcapService.credentials.apikey);
      // Try to guess at the auth type based on the properties found.
      if (StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_APIKEY))) {
        addToMap(props, Authenticator.PROPNAME_AUTH_TYPE, Authenticator.AUTHTYPE_IAM);
      } else if (StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_USERNAME))
      || StringUtils.isNotEmpty(props.get(Authenticator.PROPNAME_PASSWORD))) {
         addToMap(props, Authenticator.PROPNAME_AUTH_TYPE, Authenticator.AUTHTYPE_BASIC);
      }
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
  private static Map<String, String> parseCredentials(String serviceName, List<String> contents) {
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
