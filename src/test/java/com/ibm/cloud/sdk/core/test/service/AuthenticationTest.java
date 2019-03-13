package com.ibm.cloud.sdk.core.test.service;

import com.ibm.cloud.sdk.core.service.BaseService;
import com.ibm.cloud.sdk.core.util.CredentialUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthenticationTest {
  private static final String APIKEY_USERNAME = "apikey";
  private static final String APIKEY = "12345";
  private static final String ICP_APIKEY = "icp-12345";
  private static final String BASIC_USERNAME = "basicUser";

  public class TestService extends BaseService {
    private static final String SERVICE_NAME = "test";

    public TestService() {
      super(SERVICE_NAME);
    }
  }

  @Test
  public void authenticateWithApiKeyAsUsername() {
    TestService service = new TestService();
    service.setUsernameAndPassword(APIKEY_USERNAME, APIKEY);
    assertTrue(service.isTokenManagerSet());
  }

  @Test
  public void authenticateWithIcp() {
    TestService service = new TestService();
    service.setUsernameAndPassword(APIKEY_USERNAME, ICP_APIKEY);
    assertFalse(service.isTokenManagerSet());
  }

  @Test
  public void multiAuthenticationWithMultiBindSameServiceOnVcapService() {

    CredentialUtils.setServices("{\n"
        + "  \"test\": [\n"
        + "    {\n"
        + "      \"credentials\": {\n"
        + "        \"apikey\": \"" + APIKEY + "\",\n"
        + "        \"url\": \"https://gateway.watsonplatform.net/discovery/api\"\n"
        + "      },\n"
        + "      \"plan\": \"lite\"\n"
        + "    }\n"
        + "  ]\n"
        + "}\n");

    TestService serviceA = new TestService();
    serviceA.setUsernameAndPassword(APIKEY_USERNAME, APIKEY);

    TestService serviceB = new TestService();
    serviceB.setUsernameAndPassword(BASIC_USERNAME, APIKEY);

    assertTrue(serviceA.isTokenManagerSet());
    assertFalse(serviceB.isTokenManagerSet());
  }
}
