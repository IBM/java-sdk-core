/**
 * (C) Copyright IBM Corp. 2021.
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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions.LoggingLevel;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * VpcInstanceAuthenticator implements an authentication scheme in which it
 * retrieves an "instance identity token" and exchanges that for an IAM access
 * token using the VPC Instance Metadata Service API which is available on
 * a local VPC-managed compute resource (VM).
 * <p>The instance identity token is similar to an IAM apikey, except that it is
 * managed automatically by the compute resource provider (VPC).
 * <p>The resulting IAM access token is then added to outbound
 * requests in an Authorization header of the form: "Authorization: Bearer &lt;access-token&gt;"
 */
public class VpcInstanceAuthenticator extends AuthenticatorBase implements Authenticator {
  private static final Logger LOG = Logger.getLogger(VpcInstanceAuthenticator.class.getName());
  private static final String defaultIMSEndpoint = "http://169.254.169.254";
  private static final String operationPathCreateAccessToken = "/instance_identity/v1/token";
  private static final String operationPathCreateIamToken = "/instance_identity/v1/iam_token";
  private static final String metadataFlavor = "ibm";
  private static final String metadataServiceVersion = "2021-09-20";
  private static final int instanceIdentityTokenLifetime = 300;

  // Properties specific to a VpcInstanceAuthenticator.
  private String iamProfileCrn;
  private String iamProfileId;
  private String url;

  // The cached IAM access token and its expiration time.
  private IamToken tokenData;

  /**
   * This Builder class is used to construct IamAuthenticator instances.
   */
  public static class Builder {
    private String iamProfileCrn;
    private String iamProfileId;
    private String url;

    // Default ctor.
    public Builder() {
    }

    // Builder ctor which copies config from an existing authenticator instance.
    private Builder(VpcInstanceAuthenticator obj) {
      this.iamProfileCrn = obj.iamProfileCrn;
      this.iamProfileId = obj.iamProfileId;
      this.url = obj.url;
    }

    /**
     * Constructs a new instance of IamAuthenticator from the builder's
     * configuration.
     *
     * @return the IamAuthenticator instance
     */
    public VpcInstanceAuthenticator build() {
      return new VpcInstanceAuthenticator(this);
    }

    /**
     * Sets the iamProfileCrn property.
     *
     * @param iamProfileCrn the CRN of the linked trusted IAM profile to be used as
     *                      the identity of the compute resource. At most one of
     *                      iamProfileCrn or iamProfileId may be specified. If
     *                      neither one is specified, then the default IAM profile
     *                      defined for the compute resource will be used.
     * @return the Builder
     */
    public Builder iamProfileCrn(String iamProfileCrn) {
      this.iamProfileCrn = iamProfileCrn;
      return this;
    }

    /**
     * Sets the iamProfileId property.
     *
     * @param iamProfileId the id of the linked trusted IAM profile to be used as
     *                     the identity of the compute resource. At most one of
     *                     iamProfileCrn or iamProfileId may be specified. If
     *                     neither one is specified, then the default IAM profile
     *                     defined for the compute resource will be used.
     * @return the Builder
     */
    public Builder iamProfileId(String iamProfileId) {
      this.iamProfileId = iamProfileId;
      return this;
    }

    /**
     * Sets the url property.
     *
     * @param url the base url to use with the IAM token service
     * @return the Builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }
  }

  // The default ctor is hidden to force the use of the non-default ctors.
  protected VpcInstanceAuthenticator() {
  }

  /**
   * Constructs an IamAuthenticator instance from the configuration contained in
   * "builder".
   *
   * @param builder the Builder instance containing the configuration to be used
   */
  protected VpcInstanceAuthenticator(Builder builder) {
    this.iamProfileCrn = builder.iamProfileCrn;
    this.iamProfileId = builder.iamProfileId;
    this.url = builder.url;
    this.validate();
  }

  /**
   * Returns a new Builder instance pre-loaded with the configuration from "this".
   *
   * @return the Builder instance
   */
  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Constructs a ContainerAuthenticator instance using properties contained in
   * the specified Map.
   *
   * @param config a map containing the configuration properties
   *
   * @return the ContainerAuthenticator instance
   */
  public static VpcInstanceAuthenticator fromConfiguration(Map<String, String> config) {
    return new Builder().iamProfileCrn(config.get(PROPNAME_IAM_PROFILE_CRN))
        .iamProfileId(config.get(PROPNAME_IAM_PROFILE_ID)).url(config.get(PROPNAME_URL)).build();
  }

  @Override
  public void validate() {
    // At most one of iamProfileCrn or iamProfileId may be specified.
    if (StringUtils.isNotEmpty(getIamProfileCrn()) && StringUtils.isNotEmpty(getIamProfileId())) {
      throw new IllegalArgumentException(
          String.format(ERRORMSG_ATMOST_ONE_PROP_ERROR, "iamProfileName", "iamProfileId"));
    }
  }

  @Override
  public String authenticationType() {
    return Authenticator.AUTHTYPE_VPC;
  }

  /**
   * @return the iamProfileCrn configured on this Authenticator.
   */
  public String getIamProfileCrn() {
    return this.iamProfileCrn;
  }

  /**
   * Sets the iamProfileCrn property on this Authenticator.
   *
   * @param iamProfileCrn the value to set
   */
  protected void setIamProfileCrn(String iamProfileCrn) {
    this.iamProfileCrn = iamProfileCrn;
  }

  /**
   * @return the iamProfileId configured on this Authenticator.
   */
  public String getIamProfileId() {
    return this.iamProfileId;
  }

  /**
   * Sets the iamProfileId property on this Authenticator.
   *
   * @param iamProfileid the value to set
   */
  protected void setIamProfileId(String iamProfileId) {
    this.iamProfileId = iamProfileId;
  }

  /**
   * @return the URL configured on this Authenticator.
   */
  public String getURL() {
    return this.url;
  }

  /**
   * Sets the URL on this Authenticator.
   *
   * @param url the VPC Instance Metadata Service base endpoint URL
   */
  protected void setURL(String url) {
    if (StringUtils.isEmpty(url)) {
      url = defaultIMSEndpoint;
    }
    this.url = url;
  }

  private String getImdsEndpoint() {
    return (StringUtils.isEmpty(this.url) ? defaultIMSEndpoint : this.url);
  }

  protected void setTokenData(IamToken tokenData) {
    this.tokenData = tokenData;
  }

  /**
   * Authenticate the specified request by adding an Authorization header
   * containing a "Bearer" access token.
   */
  @Override
  public void authenticate(okhttp3.Request.Builder builder) {
    String headerValue = constructBearerTokenAuthHeader(getToken());
    if (headerValue != null) {
      builder.header(HttpHeaders.AUTHORIZATION, headerValue);
    }
  }

  /**
   * This function returns an IAM access token to be used in an Authorization
   * header. Whenever a new IAM access token is needed (when a token doesn't yet
   * exist or the existing token has expired), a new IAM access token is fetched
   * from the VPC Instance Metadata Service.
   *
   * @return the IAM access token
   */
  public String getToken() {

    if (this.tokenData == null || !this.tokenData.isTokenValid()) {
      setTokenData(synchronizedRequestToken());
    } else if (this.tokenData.needsRefresh()) {

      // Kick off background task to refresh token.
      Thread updateTokenCall = new Thread(new Runnable() {
        @Override
        public void run() {
          setTokenData(requestToken());
        }
      });
      updateTokenCall.start();
    }

    // Make sure we have a non-null tokenData object.
    // This should not occur, but just in case it does... :)
    if (this.tokenData == null) {
      throw new RuntimeException(ERRORMSG_REQ_FAILED + " illegal state: token object not available");
    }

    // Check to see if an exception occurred during our last interaction with the
    // token service.
    if (this.tokenData.getException() != null) {
      Throwable t = tokenData.getException();
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(ERRORMSG_REQ_FAILED, tokenData.getException());
      }
    }

    // Return the access token from our stored tokenData object.
    return tokenData.getAccessToken();
  }

  /**
   * Calls the extending class' requestToken implementation in a synchronized way.
   * The requestToken implementation will not be called if the stored token has
   * been made valid since this method's initial call.
   *
   * @return the token object
   */
  protected synchronized IamToken synchronizedRequestToken() {
    if (this.tokenData != null && this.tokenData.isTokenValid()) {
      return this.tokenData;
    }

    return requestToken();
  }

  /**
   * Fetches an IAM access token using the authenticator's configuration.
   *
   * @return an IamToken instance that contains the access token
   */
  public IamToken requestToken() {
    IamToken token = null;
    try {
      // Retrieve the instance identity token from the VPC Instance Metadata Service.
      String instanceIdentityToken = retrieveInstanceIdentityToken();

      // Next, exchange the instance identity token for an IAM access token.
      token = retrieveIamAccessToken(instanceIdentityToken);
    } catch (Throwable t) {
      token = new IamToken(t);
    }

    return token;
  }

  /**
   * Retrieves the local compute resource's instance identity token using
   * the "create_access_token" operation of the local VPC Instance Metadata Service API.
   * @return the instance identity token
   */
  protected String retrieveInstanceIdentityToken() throws Throwable {
    String instanceIdentityToken = null;
    try {
      // Create a PUT request to retrieve the instance identity token.
      RequestBuilder builder = RequestBuilder
          .put(RequestBuilder.resolveRequestUrl(getImdsEndpoint(), operationPathCreateAccessToken));

      // Set the params and request body.
      builder.query("version", metadataServiceVersion);
      builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
      builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_JSON);
      builder.header("Metadata-Flavor", metadataFlavor);

      String requestBody = String.format("{\"expires_in\": %d}", instanceIdentityTokenLifetime);
      builder.bodyContent(requestBody, HttpMediaType.APPLICATION_JSON);

      // Invoke the VPC IMDS "create_iam_token" operation.
      LOG.log(Level.FINE, "Invoking VPC 'create_access_token' operation: {0}", builder.toUrl());
      VpcTokenResponse vpcResponse = invokeRequest(builder, VpcTokenResponse.class);
      LOG.log(Level.FINE, "Returned from VPC 'create_access_token' operation.");

      instanceIdentityToken = vpcResponse.getAccessToken();
    } catch (Throwable t) {
      LOG.log(Level.FINE, "Caught exception from VPC 'create_access_token' operation: ", t);
      throw t;
    }

    return instanceIdentityToken;
  }

  /**
   * Retrieves the IAM access token by invoking the VPC "create_iam_token" operation.
   * @param instanceIdentityToken the current compute resource's instance identity token
   * @return the IamToken instance containing the IAM access token
   */
  protected IamToken retrieveIamAccessToken(String instanceIdentityToken) {
    IamToken iamToken = null;
    try {
      // Create a POST request to retrieve the IAM access token.
      RequestBuilder builder =
          RequestBuilder.post(RequestBuilder.resolveRequestUrl(getImdsEndpoint(), operationPathCreateIamToken));

      // Set the params and request body.
      builder.query("version", metadataServiceVersion);
      builder.header(HttpHeaders.ACCEPT, HttpMediaType.APPLICATION_JSON);
      builder.header(HttpHeaders.CONTENT_TYPE, HttpMediaType.APPLICATION_JSON);
      builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + instanceIdentityToken);

      // Next, construct the optional request body to specify the linked IAM profile.
      // We previously verified that at most one of IBMProfileCRN or IAMProfileID was specified by the user,
      // so just process them individually here and create the appropriate request body if needed.
      // If neither property was specified by the user, then no request body is sent with the request.
      String requestBody = null;
      if (!StringUtils.isEmpty(getIamProfileCrn())) {
        requestBody = String.format("{\"trusted_profile\": {\"crn\": \"%s\"}}", getIamProfileCrn());
      }
      if (!StringUtils.isEmpty(getIamProfileId())) {
        requestBody = String.format("{\"trusted_profile\": {\"id\": \"%s\"}}", getIamProfileId());
      }

      // If we created a request body above, then set it on the request now.
      if (!StringUtils.isEmpty(requestBody)) {
        builder.bodyContent(requestBody, HttpMediaType.APPLICATION_JSON);
      }

      // Invoke the VPC IMDS "create_iam_token" operation.
      LOG.log(Level.FINE, "Invoking VPC 'create_iam_token' operation: {0}", builder.toUrl());
      VpcTokenResponse vpcResponse = invokeRequest(builder, VpcTokenResponse.class);
      LOG.log(Level.FINE, "Returned from VPC 'create_iam_token' operation.");

      // Convert the response to an IamToken instance.
      iamToken = new IamToken(vpcResponse);
    } catch (Throwable t) {
      iamToken = new IamToken(t);
      LOG.log(Level.FINE, "Caught exception from VPC 'create_iam_token' operation: ", t);
    }

    return iamToken;
  }

  /**
   * Invokes the specified request and returns the response object.
   *
   * @param requestBuilder the partially-built request for fetching a token from
   *                       the token service
   * @param responseClass  a Class object which represents the token service
   *                       response structure
   * @return an instance of the response class R
   * @throws Throwable an error occurred when invoking the request
   */
  @SuppressWarnings("unchecked")
  protected <R> R invokeRequest(final RequestBuilder requestBuilder, final Class<? extends R> responseClass)
      throws Throwable {

    // Allocate the response.
    final Object[] responseObj = new Object[1];

    // Set up the Client we'll use to invoke the request.
    final HttpConfigOptions.Builder clientOptions = new HttpConfigOptions.Builder();
    if (LOG.isLoggable(Level.FINE)) {
      clientOptions.loggingLevel(LoggingLevel.BODY);
    }

    final OkHttpClient client = HttpClientSingleton.getInstance().configureClient(clientOptions.build());

    // Create the request.
    final Request request = requestBuilder.build();

    Thread restCall = new Thread(new Runnable() {
      @Override
      public void run() {
        Call call = client.newCall(request);
        ResponseConverter<R> converter = ResponseConverterUtils.getObject(responseClass);

        try {
          okhttp3.Response response = call.execute();

          // handle possible errors
          if (response.code() < 200 || response.code() >= 300) {
            throw new ServiceResponseException(response.code(), response);
          }

          // Store the API response so that we can pass it back to the main thread.
          responseObj[0] = converter.convert(response);
        } catch (Throwable t) {

          // Store the exception so that we can pass it back to the main thread.
          responseObj[0] = t;
        }
      }
    });

    restCall.start();
    try {
      restCall.join();
    } catch (Throwable t) {
      responseObj[0] = t;
    }

    // Check to see if we need to throw an exception now that we're back on the main
    // thread.
    if (responseObj[0] instanceof Throwable) {
      throw (Throwable) responseObj[0];
    }

    return (R) responseObj[0];
  }
}
