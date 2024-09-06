//
// Copyright 2024 IBM Corporation.
// SPDX-License-Identifier: Apache2.0
//

package com.ibm.cloud.sdk.core.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class LoggingUtilsTest {

  @Test
  public void testRedactHeaders() {
    assertEquals(LoggingUtils.redactSecrets("Authorization: secret"), "Authorization: [redacted]");
    assertEquals(LoggingUtils.redactSecrets("X-Authoritay: foo"), "X-Authoritay: [redacted]");
    assertEquals(LoggingUtils.redactSecrets("Content-Type: foo"), "Content-Type: foo");

    String input = "Content-Type: application/json\nAuthorization: secret\nX-Authorization: secret\nAccept: application/json";
    String expected = "Content-Type: application/json\nAuthorization: [redacted]\nX-Authorization: [redacted]\nAccept: application/json";
    assertEquals(LoggingUtils.redactSecrets(input), expected);
  }

  @Test
  public void testRedactProperties() {
    assertEquals(LoggingUtils.redactSecrets("...&apikey=foo&..."), "...&apikey=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&apiKey=foo&..."), "...&apiKey=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&passcode=mypassword&..."), "...&passcode=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&passCODE=mypassword&..."), "...&passCODE=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&aadClientSecret=mypassword&..."),
        "...&aadClientSecret=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&SECRET=asecret&..."), "...&SECRET=[redacted]&...");
    assertEquals(LoggingUtils.redactSecrets("...&thumbprint=mythumb.print&..."), "...&thumbprint=[redacted]&...");
  }

  @Test
  public void testRedactJsonFields() {
    assertEquals(LoggingUtils.redactSecrets("xxx \"apIKey\": \"secret\" xxx"), "xxx \"apIKey\":\"[redacted]\" xxx");
    assertEquals(LoggingUtils.redactSecrets("xxx \"project_ID\": \"secret\" xxx"),
        "xxx \"project_ID\":\"[redacted]\" xxx");
    assertEquals(LoggingUtils.redactSecrets("xxx \"tenantID\": \"secret\" xxx"), "xxx \"tenantID\":\"[redacted]\" xxx");
  }
}
