//
// Copyright 2024 IBM Corporation.
// SPDX-License-Identifier: Apache2.0
//

package com.ibm.cloud.sdk.core.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utilities used by our logging function.
 */
public class LoggingUtils {

  // Hide the default ctor since this is a utility class.
  private LoggingUtils() { }


  // Specific property/field names we'll try to redact in the input string.
  private static final String[] redactedKeywords = {
      "apikey",
      "api_key",
      "passcode",
      "password",
      "token",
      "aadClientId",
      "aadClientSecret",
      "auth",
      "auth_provider_x509_cert_url",
      "auth_uri",
      "client_email",
      "client_id",
      "client_x509_cert_url",
      "key",
      "project_id",
      "secret",
      "subscriptionId",
      "tenantId",
      "thumbprint",
      "token_uri", };
  private static final String redactedTokens = String.join("|", Arrays.asList(redactedKeywords));
  private static final String redacted = "[redacted]";

  private static final Pattern reAuthHeader = Pattern.compile("(?m)^(Authorization|X-Auth\\S*): .*");
  private static final Pattern rePropertySetting = Pattern.compile("(?i)(" + redactedTokens + ")=[^&]*(&|$)");
  private static final Pattern reJsonField =
      Pattern.compile("(?i)\"([^\"]*(" + redactedTokens + ")[^\"_]*)\":\\s*\"[^\\,]*\"");

  /**
   * Redacts secrets within string "s" and returns the resulting string
   * with secrets replaced by "[redacted]".
   * The resulting string should be suitable for including in debug logs.
   * @param s the string to be redacted
   * @return the input string with secrets replaced with "[redacted]"
   */
  public static String redactSecrets(String s) {
    String redactedString = s;

    // Redact secrets within any special headers.
    Matcher matcher1 = reAuthHeader.matcher(redactedString);
    redactedString = matcher1.replaceAll("$1: " + redacted);

    // Redact secrets within property settings.
    Matcher matcher2 = rePropertySetting.matcher(redactedString);
    redactedString = matcher2.replaceAll("$1=" + redacted + "$2");

    // Redact secrets within JSON strings.
    Matcher matcher3 = reJsonField.matcher(redactedString);
    redactedString = matcher3.replaceAll("\"$1\":\"" + redacted + "\"");

    return redactedString;
  }
}
