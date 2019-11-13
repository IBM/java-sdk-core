package com.ibm.cloud.sdk.core.test.security;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.cloud.sdk.core.test.BaseServiceUnitTest;
import org.junit.Before;
import org.junit.Test;

import static com.ibm.cloud.sdk.core.test.TestUtils.loadFixture;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;

public class TokenRequestBasedAuthenticatorTest extends BaseServiceUnitTest {
  private String url;
  private IamToken validTokenData;

  private static final String API_KEY = "123456789";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    url = getMockWebServerUrl();
    validTokenData = loadFixture("src/test/resources/valid_iam_token.json", IamToken.class);
  }

  @Test
  public void tokenRequestLockTest() throws InterruptedException {
    server.enqueue(jsonResponse(validTokenData));

    IamAuthenticator authenticator = new IamAuthenticator(API_KEY, url, null, null, false, null);
    final IamAuthenticator authenticatorSpy = spy(authenticator);
    final String[] accessToken = { null };

    // Run test with 20 different threads.
    for (int i = 0; i < 20; i++) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          String tokenResponse = authenticatorSpy.getToken();

          // Every token we get back should be the same.
          if (accessToken[0] == null) {
            accessToken[0] = authenticatorSpy.getToken();
          } else {
            assertEquals(accessToken[0], tokenResponse);
          }
        }
      });

      thread.start();
      thread.join();
    }

    // We should only ever request the token and make an API call once.
    verify(authenticatorSpy, times(1)).requestToken();
  }
}
