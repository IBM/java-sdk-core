//
// Copyright 2024 IBM Corporation.
// SPDX-License-Identifier: Apache2.0
//

// DISCLAIMER: The password/token strings used in this file are for testing purposes only.
// They are not real credentials and cannot be used to authenticate with any real service.

package com.ibm.cloud.sdk.core.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
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

  @Test
  public void testRedactSecretsMultiline() {
    // ── property_settings_pattern ─────────────────────────────────────────────

    // Secret keyword=value at end of line must not bleed into the next line.
    String result = LoggingUtils.redactSecrets("password=secret\nnextline");
    assertFalse(result.contains("secret"));
    assertTrue(result.contains("nextline"));

    // Two secret pairs on consecutive lines — each is independently redacted.
    result = LoggingUtils.redactSecrets("password=secret1\ntoken=secret2\nsafe=safe2");
    assertFalse(result.contains("secret1"));
    assertFalse(result.contains("secret2"));
    assertTrue(result.contains("safe=safe2"));

    // Non-secret key on the line before a secret key — must not be touched.
    result = LoggingUtils.redactSecrets("username=alice\npassword=hunter2");
    assertTrue(result.contains("username=alice"));
    assertFalse(result.contains("hunter2"));

    // Secret key embedded in a query string: value stops at & and the rest is kept.
    result = LoggingUtils.redactSecrets("password=secret&other=kept");
    assertFalse(result.contains("secret"));
    assertTrue(result.contains("other=kept"));

    // Multiple secret keys in one query string on a single line.
    result = LoggingUtils.redactSecrets("apikey=k1&password=p2&token=t3");
    assertFalse(result.contains("k1"));
    assertFalse(result.contains("p2"));
    assertFalse(result.contains("t3"));

    // Non-secret key: must be left untouched.
    result = LoggingUtils.redactSecrets("username=alice");
    assertTrue(result.contains("username=alice"));

    // Secret keyword=value preceded by unrelated text on the same line.
    result = LoggingUtils.redactSecrets("grant_type=urn:ietf:params:oauth:grant-type:iam-authz&apikey=mysecret");
    assertFalse(result.contains("mysecret"));
    assertTrue(result.contains("grant_type=urn"));

    // ── json_field_pattern ────────────────────────────────────────────────────

    // Secret JSON field at end of line must not consume the next line.
    result = LoggingUtils.redactSecrets("\"password\": \"secret\"\n\"other\": \"value\"");
    assertFalse(result.contains("secret"));
    assertTrue(result.contains("\"other\": \"value\""));

    // Two secret JSON fields on consecutive lines — each redacted independently.
    result = LoggingUtils.redactSecrets(
        "{\n  \"password\": \"secret1\",\n  \"token\": \"secret2\",\n  \"name\": \"alice\"\n}");
    assertFalse(result.contains("secret1"));
    assertFalse(result.contains("secret2"));
    assertTrue(result.contains("\"name\": \"alice\""));

    // Non-secret JSON field on the line before a secret field — must be preserved.
    result = LoggingUtils.redactSecrets("\"username\": \"alice\"\n\"password\": \"hunter2\"");
    assertTrue(result.contains("\"username\": \"alice\""));
    assertFalse(result.contains("hunter2"));

    // Non-secret JSON field on the line after a secret field — must be preserved.
    result = LoggingUtils.redactSecrets("\"password\": \"hunter2\"\n\"username\": \"alice\"");
    assertFalse(result.contains("hunter2"));
    assertTrue(result.contains("\"username\": \"alice\""));

    // Secret JSON field with surrounding non-secret fields on the same line.
    result = LoggingUtils.redactSecrets("{\"name\": \"alice\", \"password\": \"s3cr3t\", \"role\": \"admin\"}");
    assertFalse(result.contains("s3cr3t"));
    assertTrue(result.contains("\"name\": \"alice\""));
    assertTrue(result.contains("\"role\": \"admin\""));

    // ── auth_header_pattern ───────────────────────────────────────────────────

    // Authorization header at EOL must not consume the line that follows.
    result = LoggingUtils.redactSecrets("Authorization: Bearer tok\nContent-Type: application/json");
    assertFalse(result.contains("tok"));
    assertTrue(result.contains("Content-Type: application/json"));

    // Two auth headers on consecutive lines — each redacted, body line preserved.
    result = LoggingUtils.redactSecrets("Authorization: Bearer tok1\nX-Auth-Token: tok2\nbody");
    assertFalse(result.contains("tok1"));
    assertFalse(result.contains("tok2"));
    assertTrue(result.contains("body"));
  }
}
