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

package com.ibm.cloud.sdk.core.http;

import java.util.List;
import java.util.Set;

/**
 * Wrapper class for the internal HTTP headers class.
 */
public class Headers {

  private okhttp3.Headers headers;

  public Headers(okhttp3.Headers headers) {
    this.headers = headers;
  }

  /**
   * Returns true if other is a Headers object with the same headers, with the same casing, in the same order.
   *
   * @param other the other object to compare
   * @return whether the two objects are equal or not
   */
  @Override
  public boolean equals(Object other) {
    return this.headers.equals(other);
  }

  @Override
  public int hashCode() {
    return this.headers.hashCode();
  }

  @Override
  public String toString() {
    return this.headers.toString();
  }

  /**
   * Returns an immutable, case-insensitive set of header names.
   *
   * @return the list of header names
   */
  public Set<String> names() {
    return this.headers.names();
  }

  /**
   * Returns an immutable list of the header values for the specified name.
   *
   * @param name the name of the specified header
   * @return the values associated with the name
   */
  public List<String> values(String name) {
    return this.headers.values(name);
  }
}
