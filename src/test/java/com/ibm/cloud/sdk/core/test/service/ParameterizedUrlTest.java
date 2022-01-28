/**
 * (C) Copyright IBM Corp. 2021, 2022.  All Rights Reserved.
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

package com.ibm.cloud.sdk.core.test.service;

import com.ibm.cloud.sdk.core.service.BaseService;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests associated with constructing a service URL from a parameterized URL.
 */
public class ParameterizedUrlTest {

  private static final String parameterizedUrl = "{scheme}://{domain}:{port}";
  private static final Map<String, String> defaultUrlVariables = createDefaultUrlVariables();

  private static Map<String, String> createDefaultUrlVariables() {

    Map<String, String> map = new HashMap<>();
    map.put("scheme", "http");
    map.put("domain", "ibm.com");
    map.put("port", "9300");

    return map;
  }

  @Test
  public void testConstructServiceURLWithNull() {
    String constructedUrl = BaseService.constructServiceUrl(parameterizedUrl, defaultUrlVariables, null);
    assertEquals(constructedUrl, "http://ibm.com:9300");
  }

  @Test
  public void testConstructServiceURLWithSomeProvidedVariables() {

    Map<String, String> providedUrlVariables = new HashMap<>();
    providedUrlVariables.put("scheme", "https");
    providedUrlVariables.put("port", "22");

    String constructedUrl = BaseService.constructServiceUrl(
      parameterizedUrl, defaultUrlVariables, providedUrlVariables
    );
    assertEquals(constructedUrl, "https://ibm.com:22");
  }

  @Test
  public void testConstructServiceURLWithAllProvidedVariables() {

    Map<String, String> providedUrlVariables = new HashMap<>();
    providedUrlVariables.put("scheme", "https");
    providedUrlVariables.put("domain", "google.com");
    providedUrlVariables.put("port", "22");

    String constructedUrl = BaseService.constructServiceUrl(
      parameterizedUrl, defaultUrlVariables, providedUrlVariables
    );
    assertEquals(constructedUrl, "https://google.com:22");
  }

  @SuppressWarnings("deprecation")
  @Test(
    expectedExceptions = IllegalArgumentException.class,
    expectedExceptionsMessageRegExp =
    "'server' is an invalid variable name\\."
    + "\nValid variable names: \\[domain, port, scheme\\]\\."
  )
  public void testConstructServiceURLWithInvalidVariable() {

    Map<String, String> providedUrlVariables = new HashMap<>();
    providedUrlVariables.put("server", "value");

    BaseService.constructServiceURL(parameterizedUrl, defaultUrlVariables, providedUrlVariables);
  }
}
