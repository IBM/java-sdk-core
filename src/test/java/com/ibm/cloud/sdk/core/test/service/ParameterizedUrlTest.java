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

  @Test(
    expectedExceptions = IllegalArgumentException.class, 
    expectedExceptionsMessageRegExp = 
    "'server' is an invalid variable name\\."
    + "\nValid variable names: \\[domain, port, scheme\\]\\." 
  )
  public void testConstructServiceURLWithInvalidVariable() {

    Map<String, String> providedUrlVariables = new HashMap<>();
    providedUrlVariables.put("server", "value");

    BaseService.constructServiceUrl(parameterizedUrl, defaultUrlVariables, providedUrlVariables);
  }
}
