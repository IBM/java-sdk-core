/**
 * (C) Copyright IBM Corp. 2019.
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

package com.ibm.cloud.sdk.core.security;

/**
 * This class models a response received from the CP4D "get token" API.
 */
public class Cp4dTokenResponse implements TokenServerResponse {

  private String username;
  private String role;
  private String[] permissions;
  private String sub;
  private String iss;
  private String aud;
  private String uid;
  private String accessToken;
  private String _messageCode_;
  private String message;

  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getRole() {
    return role;
  }
  public void setRole(String role) {
    this.role = role;
  }
  public String[] getPermissions() {
    return permissions;
  }
  public void setPermissions(String[] permissions) {
    this.permissions = permissions;
  }
  public String getSub() {
    return sub;
  }
  public void setSub(String sub) {
    this.sub = sub;
  }
  public String getIss() {
    return iss;
  }
  public void setIss(String iss) {
    this.iss = iss;
  }
  public String getAud() {
    return aud;
  }
  public void setAud(String aud) {
    this.aud = aud;
  }
  public String getUid() {
    return uid;
  }
  public void setUid(String uid) {
    this.uid = uid;
  }
  public String getAccessToken() {
    return accessToken;
  }
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
  public String get_messageCode_() {
    return _messageCode_;
  }
  public void set_messageCode_(String messageCode) {
    this._messageCode_ = messageCode;
  }
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }

}
