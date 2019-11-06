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

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.InputStreamRequestBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions to use when creating a {@link com.ibm.cloud.sdk.core.http.RequestBuilder }.
 *
 */
public final class RequestUtils {
  private static final Logger LOG = Logger.getLogger(RequestUtils.class.getName());
  private static final String[] properties =
      new String[] { "java.vendor", "java.version", "os.arch", "os.name", "os.version" };
  private static final String VERSION_PROPS_FILENAME = "sdk-core-version.properties";
  private static String userAgent;

  private RequestUtils() {
    // This is a utility class - no instantiation allowed.
  }

  /**
   * Encode a string into a valid URL string.
   *
   * @param content the content
   * @return the string
   */
  public static String encode(String content) {
    try {
      return URLEncoder.encode(content, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Return a copy of a {@link Map} excluding the given key, or array of keys.
   *
   * @param params the parameters
   * @param toOmit the keys to omit
   * @return the map with the omitted key-value pars, or null if params is null
   */
  public static Map<String, Object> omit(Map<String, Object> params, String... toOmit) {
    if (params == null) {
      return null;
    }

    final Map<String, Object> ret = new HashMap<String, Object>(params);

    if (toOmit != null) {
      ret.keySet().removeAll(Arrays.asList(toOmit));
    }

    return ret;
  }


  /**
   * Return a copy of a {@link Map} with only the specified given key, or array of keys. If {@code toPick} is empty all
   * keys will remain in the Map.
   *
   * @param params the parameters
   * @param toPick the keys to pick
   * @return the map with the picked key-value pars, or null if params is null
   */

  public static Map<String, Object> pick(Map<String, Object> params, String... toPick) {
    if (params == null) {
      return null;
    }

    final Map<String, Object> ret = new HashMap<String, Object>(params);

    if ((toPick != null) && (toPick.length > 0)) {
      ret.keySet().retainAll(Arrays.asList(toPick));
    }

    return ret;
  }

  /**
   * Creates a String of all elements of an array, separated by a separator.
   *
   * @param <T> the generic type
   * @param array the array
   * @param separator the separator
   * @return the joined String
   */
  public static <T> String join(T[] array, String separator) {
    return join(Arrays.asList(array), separator);
  }

  /**
   * Creates a String of all elements of an iterable, separated by a separator.
   *
   * @param iterable the iterable
   * @param separator the separator
   * @return the joined String
   */
  public static String join(Iterable<?> iterable, String separator) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (Object item : iterable) {
      if (first) {
        first = false;
      } else {
        sb.append(separator);
      }

      sb.append(item.toString());
    }

    return sb.toString();
  }

  /**
   * Retrieve the library's version from the sdk-core-version.properties file.
   * @return the core library's version number
   */
  private static String loadCoreVersion() {
    try (InputStream is = RequestUtils.class.getClassLoader().getResourceAsStream(VERSION_PROPS_FILENAME)) {
      Properties properties = new Properties();
      properties.load(is);
      return properties.getProperty("version", "unknown-version");
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Could not load file: " + VERSION_PROPS_FILENAME, e);
      return "unknown-version";
    }
  }

  /**
   * Builds the user agent using System properties.
   *
   * @return the string that represents the user agent
   */
  private static String buildUserAgent() {
    final List<String> details = new ArrayList<>();
    for (String propertyName : properties) {
      details.add(propertyName + "=" + System.getProperty(propertyName));
    }

    return "ibm-java-sdk-core-" + loadCoreVersion() + " " + getSystemInfo();
  }

  public static String getSystemInfo() {
    final List<String> details = new ArrayList<>();
    for (String propertyName : properties) {
      details.add(propertyName + "=" + System.getProperty(propertyName));
    }

    return "(" + RequestUtils.join(details, "; ") + ")";
  }

  /**
   * Gets the user agent.
   *
   * @return the user agent
   */
  public static synchronized String getUserAgent() {
    if (userAgent == null) {
      userAgent = buildUserAgent();
    }
    return userAgent;
  }

  /**
   * Returns a request body that encapsulates the specified file qualified with the specified content type.
   *
   * @param file the file content to POST/PUT
   * @param contentType the HTTP contentType to use.
   *
   * @return {@link RequestBody}
   */
  public static RequestBody fileBody(File file, String contentType) {
    MediaType mediaType = (contentType != null) ? MediaType.parse(contentType) : HttpMediaType.BINARY_FILE;
    return RequestBody.create(mediaType, file);
  }

  /**
   * Returns a request body the encapsulates the specified input stream qualified with the specified content type.
   *
   * @param stream the input stream content to POST/PUT
   * @param contentType the HTTP contentType to use.
   *
   * @return {@link RequestBody}
   */
  public static RequestBody inputStreamBody(InputStream stream, String contentType) {
    MediaType mediaType = (contentType != null) ? MediaType.parse(contentType) : HttpMediaType.BINARY_FILE;
    return InputStreamRequestBody.create(mediaType, stream);
  }
}
