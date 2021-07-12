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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class UrlHelperTest {

    @Test
    public void testRelativeUrl() {
        String nextURL = "/api/v1/offerings?start=foo&limit=10";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertEquals("foo", next);
    }

    @Test
    public void testAbsoluteUrl() {
        String nextURL = "https://acme.com/api/v1/offerings?start=bar&limit=10";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertEquals("bar", next);
    }

    @Test
    public void testMissingParam() {
        String nextURL = "https://acme.com/api/v1/offerings?start=bar&limit=10";
        String next = UrlHelper.getQueryParam(nextURL, "token");
        assertNull(next);
    }

    @Test
    public void testNullUrl() {
        String nextURL = null;
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertNull(next);
    }

    @Test
    public void testEmptyUrl() {
        String nextURL = "";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertNull(next);
    }

    @Test
    public void testBadUrl() {
        String nextURL = "https://foo.bar:baz/api/v1/offerings?start=foo";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertNull(next);
    }

    @Test
    public void testNoQueryString() {
        String nextURL = "/api/v1/offerings";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertNull(next);
    }

    @Test
    public void testBadQueryString() {
        String nextURL = "/api/v1/offerings?start%XXfoo";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertNull(next);
    }

    @Test
    public void testDuplicateParam() {
        String nextURL = "/api/v1/offerings?start=foo&start=bar&limit=10";
        String next = UrlHelper.getQueryParam(nextURL, "start");
        assertEquals("foo", next);
    }
}
