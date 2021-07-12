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

package com.ibm.cloud.sdk.core.test.http;

import com.ibm.cloud.sdk.core.http.HttpClientSingleton;
import com.ibm.cloud.sdk.core.http.HttpConfigOptions;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;

import static org.testng.Assert.assertTrue;

/**
 * Unit tests for the HttpClientSingleton object.
 */
public class HttpClientSingletonTest {

    private void getAndAssertEnabledProtocols(OkHttpClient client) throws IOException {
        SSLSocket sslSocket = (SSLSocket) client.sslSocketFactory().createSocket();
        List<String> enabledProtocols = Arrays.asList(sslSocket.getEnabledProtocols());
        // Assert there is at least 1 enabled protocol
        assertTrue(enabledProtocols.size() > 0, "There should be at least 1 TLS protocol enabled.");

        // Get the MODERN_TLS Java names and the runtime supported protocols
        List<String> modernTlsNames = new ArrayList<>();
        for (TlsVersion tlsVersion : ConnectionSpec.MODERN_TLS.tlsVersions()) {
            modernTlsNames.add(tlsVersion.javaName());
        }
        List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
        // Iterate and assert that each enabled protocol is present in both lists
        for (String protocol : enabledProtocols) {
            // Assert that the enabled protocols is supported by the runtime
            assertTrue(supportedProtocols.contains(protocol),
                String.format("The enabled protocol %s should be supported by the runtime", protocol));
            // Assert that the enabled protocols is present in MODERN_TLS
            assertTrue(modernTlsNames.contains(protocol),
                String.format("The enabled protocol %s should be in the MODERN_TLS connection spec", protocol));
        }
    }


    @Test
    public void testTlsProtocolFiltering() throws IOException {
        OkHttpClient client = HttpClientSingleton.getInstance().createHttpClient();
        getAndAssertEnabledProtocols(client);
    }

    @Test
    public void testTlsProtocolFilteringWithVerificationDisabled() throws IOException {
        HttpConfigOptions configOptions = new HttpConfigOptions.Builder()
            .disableSslVerification(true)
            .build();
        OkHttpClient client = HttpClientSingleton.getInstance().configureClient(configOptions);
        getAndAssertEnabledProtocols(client);
    }

}
