//
// Copyright 2024 IBM Corporation.
// SPDX-License-Identifier: Apache2.0
//

package com.ibm.cloud.sdk.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

  // Names (lower-case) of HTTP headers whose values must never be logged.
  // In addition to these, any header whose name starts with "x-auth" is also redacted.
  // NOTE: keep this list consistent with the other SDK cores (Go, Node, Python).
  private static final Set<String> redactedHeaderNames = new HashSet<>(Arrays.asList(
      "authorization",
      "proxy-authorization",
      "cookie",
      "set-cookie",
      "secret"));
  private static final String redactedHeaderPrefix = "x-auth";

  // Matches a single "Name: value" header line (line terminators are not consumed).
  private static final Pattern reHeaderLine = Pattern.compile("(?m)^([!#$%&'*+.^_`|~0-9A-Za-z-]+):[^\\r\\n]*");
  private static final Pattern rePropertySetting = Pattern.compile("(?im)(" + redactedTokens + ")=[^&\\n]*(&|$)");
  private static final Pattern reJsonField =
      Pattern.compile("(?i)\"([^\"]*(" + redactedTokens + ")[^\"_]*)\":\\s*\"[^\"\\n]*\"");

  /**
   * Returns true iff the header "name" (compared case-insensitively) is one whose value must be redacted.
   */
  private static boolean isRedactedHeader(String name) {
    String lowerName = name.toLowerCase(Locale.ROOT);
    return redactedHeaderNames.contains(lowerName) || lowerName.startsWith(redactedHeaderPrefix);
  }

  /**
   * Redacts the value of each credential-bearing HTTP header found in "s".
   * Header names are parsed line-by-line and compared case-insensitively.
   */
  private static String redactHeaders(String s) {
    Matcher matcher = reHeaderLine.matcher(s);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String name = matcher.group(1);
      String replacement = isRedactedHeader(name) ? name + ": " + redacted : matcher.group();
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

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
    redactedString = redactHeaders(redactedString);

    // Redact secrets within property settings.
    Matcher matcher2 = rePropertySetting.matcher(redactedString);
    redactedString = matcher2.replaceAll("$1=" + redacted + "$2");

    // Redact secrets within JSON strings.
    Matcher matcher3 = reJsonField.matcher(redactedString);
    redactedString = matcher3.replaceAll("\"$1\":\"" + redacted + "\"");

    return redactedString;
  }
}
