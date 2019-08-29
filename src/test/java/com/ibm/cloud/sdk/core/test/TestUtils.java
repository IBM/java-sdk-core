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

package com.ibm.cloud.sdk.core.test;

import com.ibm.cloud.sdk.core.util.GsonSingleton;
import org.junit.Ignore;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The Class TestUtils.
 */
@Ignore
public final class TestUtils {
  /** The Constant DELETE. */
  public static final String DELETE = "DELETE";

  /** The Constant HEAD. */
  public static final String HEAD = "HEAD";

  /** The Constant GET. */
  public static final String GET = "GET";

  /** The Constant POST. */
  public static final String POST = "POST";

  /** The Constant PUT. */
  public static final String PUT = "PUT";

  /**
   * Private constructor.
   */
  private TestUtils() { }

  /**
   * Gets the string from input stream.
   *
   * @param is the input stream
   * @return the string from input stream
   */
  public static String getStringFromInputStream(InputStream is) {
    BufferedReader br = null;
    final StringBuilder sb = new StringBuilder();

    String line;
    try {

      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }

    return sb.toString();

  }

  /**
   * Loads fixture.
   *
   * @param <T> the return type
   * @param filename the file name
   * @param returnType the return type
   * @return the t
   * @throws FileNotFoundException the file not found exception
   */
  public static <T> T loadFixture(String filename, Class<T> returnType) throws FileNotFoundException {
    String jsonString = getStringFromInputStream(new FileInputStream(filename));
    return GsonSingleton.getGsonWithoutPrettyPrinting().fromJson(jsonString, returnType);
  }
}
