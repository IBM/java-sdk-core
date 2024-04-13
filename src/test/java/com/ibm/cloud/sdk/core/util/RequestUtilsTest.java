/**
 * (C) Copyright IBM Corp. 2015, 2024.
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

import com.ibm.cloud.sdk.core.http.HttpMediaType;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * The Class RequestUtilsTest.
 *
 */
public class RequestUtilsTest {

  /**
   * Creates the map.
   *
   * @return the map
   */
  private Map<String, Object> createMap() {
    final Map<String, Object> params = new HashMap<String, Object>();
    params.put("A", 1);
    params.put("B", 2);
    params.put("C", 3);
    params.put("D", 4);
    return params;
  }

  /**
   * Test omit.
   */
  @Test
  public void testOmit() {
    final Map<String, Object> params = createMap();


    Map<String, Object> omitted = RequestUtils.omit(params, "A");

    assertTrue(omitted.keySet().containsAll(Arrays.asList("B", "C", "D")));
    assertTrue(omitted.values().containsAll(Arrays.asList(2, 3, 4)));

    omitted = RequestUtils.omit(params, "F");
    assertTrue(omitted.keySet().containsAll(Arrays.asList("A", "B", "C", "D")));
    assertTrue(omitted.values().containsAll(Arrays.asList(1, 2, 3, 4)));
  }

  /**
   * Test omit with nulls.
   */
  @Test
  public void testOmitWithNulls() {
    final Map<String, Object> params = createMap();

    assertEquals(params.keySet().toArray(), RequestUtils.omit(params).keySet().toArray());
    assertEquals(params.values().toArray(), RequestUtils.omit(params).values().toArray());

    assertNull(RequestUtils.omit(null));
  }

  @Test
  public void testEncode() {
    assertEquals(RequestUtils.encode("foo"), "foo");
    assertEquals(RequestUtils.encode("foo#3%3"), "foo%233%253");
  }

  @Test
  public void testJoin() {
    String[] strings = { "foo", "bar" };
    Integer[] ints = { 38, 28, 36 };
    assertEquals(RequestUtils.join(strings, ","), "foo,bar");
    assertEquals(RequestUtils.join(ints, ":"), "38:28:36");
  }

  /**
   * Test pick.
   */
  @Test
  public void testPick() {
    final Map<String, Object> params = createMap();

    Map<String, Object> picked = RequestUtils.pick(params, "A");
    assertEquals(picked.keySet().toArray(), new String[] { "A" });
    assertEquals(picked.values().toArray(), new Integer[] { 1 });

    picked = RequestUtils.pick(params, "F");
    assertEquals(picked.keySet().toArray(), new String[] { });
    assertEquals(picked.values().toArray(), new Integer[] { });
  }

  /**
   * Test pick with nulls.
   */
  @Test
  public void testPickWithNulls() {
    final Map<String, Object> params = createMap();

    assertEquals(params.keySet().toArray(), RequestUtils.pick(params).keySet().toArray());
    assertEquals(params.values().toArray(), RequestUtils.pick(params).values().toArray());

    assertNull(RequestUtils.pick(null));
  }

  /**
   * Test user agent.
   */
  @Test
  public void testUserAgent() {
    assertNotNull(RequestUtils.getUserAgent());
    assertTrue(RequestUtils.getUserAgent().startsWith("ibm-java-sdk-core-"));
  }

  @Test
  public void testBuildUserAgent() {
    assertTrue(RequestUtils.buildUserAgent(null).startsWith("ibm-java-sdk-core-"));
    assertTrue(RequestUtils.buildUserAgent("sub-component").startsWith("ibm-java-sdk-core/sub-component-"));
  }

  @Test
  public void testFileBody() {
    File f = new File("src/test/resources/cr-token.txt");
    okhttp3.RequestBody body = RequestUtils.fileBody(f, null);
    assertNotNull(body);
    assertEquals(HttpMediaType.BINARY_FILE, body.contentType());

    body = RequestUtils.fileBody(f, "application/octet-stream");
    assertNotNull(body);
    assertEquals(HttpMediaType.BINARY_FILE, body.contentType());
  }

  @Test
  public void testInputStreamBody() throws Exception {
    InputStream is = new FileInputStream("src/test/resources/cr-token.txt");
    okhttp3.RequestBody body = RequestUtils.inputStreamBody(is, null);
    assertNotNull(body);
    assertEquals(HttpMediaType.BINARY_FILE, body.contentType());

    body = RequestUtils.inputStreamBody(is, "application/octet-stream");
    assertNotNull(body);
    assertEquals(HttpMediaType.BINARY_FILE, body.contentType());
  }
}
