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

package com.ibm.cloud.sdk.core.util;

import okhttp3.HttpUrl;

public class UrlHelper {

    private UrlHelper() {
    }

    /**
     * Returns the value of query parameter `param` from urlStr, or null when not found.
     * @param urlStr the URL string containing the query param to retrieve
     * @param param the name of the query param whose value should be returned
     * @return the value of the specified query parameter
     */
    public static String getQueryParam(String urlStr, String param) {
        if (urlStr != null) {
            // parse requires an absolute URL, so handle relative URLs by adding dummy scheme and host
            if (urlStr.startsWith("/")) {
                urlStr = "https://foo.bar.com" + urlStr;
            }
            HttpUrl url = HttpUrl.parse(urlStr);
            if (url != null) {
                return url.queryParameter(param);
            }
        }
        return null;
    }
}
