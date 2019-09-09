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

package com.ibm.cloud.sdk.core.test.service;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.NoAuthAuthenticator;
import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.assertTrue;

public class HeadersTest extends BaseServiceUnitTest {

  private class TestModel extends GenericModel { }

  public class TestService extends BaseService {

    private static final String SERVICE_NAME = "test";

    public TestService(Authenticator auth) {
      super(SERVICE_NAME, auth);
    }

    public ServiceCall<TestModel> testMethod() {
      RequestBuilder builder = RequestBuilder.get(HttpUrl.parse(getServiceUrl() + "/v1/test"));
      return createServiceCall(builder.build(), ResponseConverterUtils.getObject(TestModel.class));
    }
  }

  private TestService service;

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.cloud.sdk.core.test.WatsonServiceTest#setUp()
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    service = new TestService(new NoAuthAuthenticator());
    service.setServiceUrl(getMockWebServerUrl());
  }

  /**
   * Test adding a custom header to a request.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void testAddHeader() throws InterruptedException {
    server.enqueue(new MockResponse()
        .addHeader(CONTENT_TYPE, HttpMediaType.APPLICATION_JSON)
        .setBody("{\"test_key\": \"test_value\"}"));

    String headerName = "X-Test";
    service.testMethod()
        .addHeader(headerName, "test")
        .execute();
    final RecordedRequest request = server.takeRequest();
    assertTrue(request.getHeader(headerName) != null);
  }
}
