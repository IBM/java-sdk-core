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
 * This class defines a common base class for tokens returned by a token server.
 */
public abstract class AbstractToken {
  public abstract boolean isTokenValid();
  public abstract boolean needsRefresh();
  public abstract String getAccessToken();

  // This field will be used to indicate that the most recent interaction with the token server
  // resulted in an error.
  private transient Throwable exception;

  public AbstractToken() {
  }

  public AbstractToken(Throwable t) {
    this.exception = t;
  }

  public Throwable getException() {
    return exception;
  }
  public void setException(Throwable exception) {
    this.exception = exception;
  }
}
