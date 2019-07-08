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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CredentialUtils retrieves service credentials from the environment.
 */
public final class CredentialUtils {

  /**
   * A util class to easily store service credentials.
   *
   */
  public static class ServiceCredentials {
    private String username;
    private String password;
    private String oldApiKey;
    private String url;
    private String iamApiKey;
    private String iamUrl;
    private String accessToken;

    private ServiceCredentials() { }

    private ServiceCredentials(String username, String password, String oldApiKey, String url, String iamApiKey,
                               String iamUrl) {
      this.username = username;
      this.password = password;
      this.oldApiKey = oldApiKey;
      this.url = url;
      this.iamApiKey = iamApiKey;
      this.iamUrl = iamUrl;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
      return username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
      return password;
    }

    /**
     * Gets the API used for older service instances.
     *
     * @return the oldApiKey
     */
    public String getOldApiKey() {
      return oldApiKey;
    }

    /**
     * Gets the API URL.
     *
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * Gets the IAM API key.
     *
     * @return the iamApiKey
     */
    public String getIamApiKey() {
      return iamApiKey;
    }

    /**
     * Gets the IAM URL.
     *
     * @return the iamUrl
     */
    public String getIamUrl() {
      return iamUrl;
    }

    /**
     * Gets the access token.
     *
     * @return the accessToken
     */
    public String getAccessToken() {
      return accessToken;
    }

    /**
     * Returns true if no fields are set on the object.
     *
     * @return whether the object has any set fields
     */
    public boolean isEmpty() {
      return (username == null
          && password == null
          && oldApiKey == null
          && url == null
          && iamApiKey == null
          && iamUrl == null);
    }
  }

  public static final String PLAN_STANDARD = "standard";

  private static String services;
  private static Context context;
  private static final Logger log = Logger.getLogger(CredentialUtils.class.getName());

  private static final String DEFAULT_CREDENTIAL_FILE_NAME = "ibm-credentials.env";

  private static final String VCAP_SERVICES = "VCAP_SERVICES";
  private static final String LOOKUP_NAME_EXTENSION_API_KEY = "/credentials";
  private static final String LOOKUP_NAME_EXTENSION_URL = "/url";

  private static final String CREDENTIALS = "credentials";
  private static final String PLAN = "plan";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String OLD_APIKEY = "api_key";
  private static final String URL = "url";
  private static final String IAM_APIKEY = "iam_apikey";
  // this value was used previously for IAM API keys as well
  private static final String APIKEY = "apikey";
  private static final String IAM_URL = "iam_url";
  private static final String ACCESS_TOKEN = "access_token";

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
   * Calls methods to parse VCAP_SERVICES and retrieve credential values. For some values, if VCAP_SERVICES aren't
   * present, it'll fall back to checking JNDI.
   *
   * @param serviceName the service name
   * @return ServiceCredentials object containing parsed values
   */
  public static ServiceCredentials getCredentialsFromVcap(String serviceName) {
    String username = getVcapValue(serviceName, USERNAME);
    String password = getVcapValue(serviceName, PASSWORD);
    String oldApiKey = getVcapValue(serviceName, OLD_APIKEY);
    if (username == null && password == null && oldApiKey == null) {
      oldApiKey = getJndiValue(serviceName, LOOKUP_NAME_EXTENSION_API_KEY);
    }

    String url = getVcapValue(serviceName, URL);
    if (url == null) {
      url = getJndiValue(serviceName, LOOKUP_NAME_EXTENSION_URL);
    }

    String iamApiKey = getVcapValue(serviceName, IAM_APIKEY);
    String iamUrl = getVcapValue(serviceName, IAM_URL);

    return new ServiceCredentials(username, password, oldApiKey, url, iamApiKey, iamUrl);
  }

  /**
   * Builds the lookup name to be searched for in JNDI
   * and uses it to call the overloaded JNDI method.
   *
   * @param serviceName Name of the IBM Cloud service
   * @param lookupNameExtension Extension to determine which value should be retrieved through JNDI
   * @return The encoded desired value
   */
  private static String getJndiValue(String serviceName, String lookupNameExtension) {
    return getJndiValue("watson-developer-cloud/" + serviceName + lookupNameExtension);
  }

  /**
   * Attempt to get the Base64-encoded value through JNDI.
   *
   * This method should always return null on Android due to the javax functions being unsupported
   *
   * @param lookupName Key to lookup in JNDI
   * @return The encoded desired value
   */
  private static String getJndiValue(String lookupName) {
    if (!isClassAvailable("javax.naming.Context") || !isClassAvailable("javax.naming.InitialContext")) {
      log.info("JNDI string lookups is not available.");
      return null;
    }

    try {
      if (context == null) {
        context = new InitialContext();
      }
      return (String) context.lookup(lookupName);
    } catch (Exception e) {
      log.fine("JNDI " + lookupName + " not found.");
      return null;
    }
  }

  private static boolean isClassAvailable(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * Gets the <b>VCAP_SERVICES</b> environment variable and return it as a {@link JsonObject}.
   *
   * @return the VCAP_SERVICES as a {@link JsonObject}.
   */
  private static JsonObject getVcapServices() {
    final String envServices = services != null ? services : System.getenv(VCAP_SERVICES);
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

  /**
   * Sets the VCAP_SERVICES variable. This is utility variable for testing
   *
   * @param services the VCAP_SERVICES
   */
  public static void setServices(String services) {
    CredentialUtils.services = services;
  }

  /**
   * Sets the context variable for JNDI. This is a utility method for testing.
   *
   * @param env Configuration options for the context
   */
  public static void setContext(Hashtable<String, String> env) {
    try {
      CredentialUtils.context = new InitialContext(env);
    } catch (Exception e) {
      log.fine("Error setting up JNDI context: " + e.getMessage());
    }
  }

  // Credential file-related methods

  /**
   * Calls methods to find and parse a credential file in various locations.
   *
   * @param serviceName the service name
   * @return ServiceCredentials object containing parsed values
   */
  public static ServiceCredentials getFileCredentials(String serviceName) {
    List<File> files = getFilesToCheck();
    List<String> credentialFileContents = getFirstExistingFileContents(files);
    return setCredentialFields(serviceName, credentialFileContents);
  }

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

    String userSpecifiedPath = System.getenv("IBM_CREDENTIALS_FILE");
    String unixHomeDirectory = System.getenv("HOME");
    String windowsFirstHomeDirectory = System.getenv("HOMEDRIVE") + System.getenv("HOMEPATH");
    String windowsSecondHomeDirectory = System.getenv("USERPROFILE");
    String projectDirectory = System.getProperty("user.dir");

    if (userSpecifiedPath != null) {
      files.add(new File(userSpecifiedPath));
    }
    files.add(new File(String.format("%s/%s", unixHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    files.add(new File(String.format("%s/%s", windowsFirstHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    files.add(new File(String.format("%s/%s", windowsSecondHomeDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));
    files.add(new File(String.format("%s/%s", projectDirectory, DEFAULT_CREDENTIAL_FILE_NAME)));

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
   * Parses provided list of strings to create and set values for a ServiceCredentials instance.
   *
   * @param serviceName the service name
   * @param credentialFileContents list of lines in the user's credential file
   * @return ServiceCredentials object containing the parsed values
   */
  private static ServiceCredentials setCredentialFields(String serviceName, List<String> credentialFileContents) {
    ServiceCredentials serviceCredentials = new ServiceCredentials();

    if (credentialFileContents == null) {
      return serviceCredentials;
    }

    for (String line : credentialFileContents) {
      String[] keyAndVal = line.split("=");
      String lowercaseKey = keyAndVal[0].toLowerCase();
      if (lowercaseKey.contains(serviceName)) {
        String credentialType = lowercaseKey.substring(serviceName.length() + 1);
        String credentialValue = keyAndVal[1];

        switch (credentialType) {
          case USERNAME:
            serviceCredentials.username = credentialValue;
            break;
          case PASSWORD:
            serviceCredentials.password = credentialValue;
            break;
          case OLD_APIKEY:
            serviceCredentials.oldApiKey = credentialValue;
            break;
          case URL:
            serviceCredentials.url = credentialValue;
            break;
          case APIKEY:
          case IAM_APIKEY:
            serviceCredentials.iamApiKey = credentialValue;
            break;
          case IAM_URL:
            serviceCredentials.iamUrl = credentialValue;
            break;
          case ACCESS_TOKEN:
            serviceCredentials.accessToken = credentialValue;
            break;
          default:
            log.warning("Unknown credential key found in credential file: " + credentialType);
            break;
        }
      }
    }

    return serviceCredentials;
  }
}
