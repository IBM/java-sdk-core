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

package com.ibm.cloud.sdk.core.util;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

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

    assertTrue(omitted.keySet().containsAll(Lists.newArrayList("B", "C", "D")));
    assertTrue(omitted.values().containsAll(Lists.newArrayList(2, 3, 4)));

    omitted = RequestUtils.omit(params, "F");
    assertTrue(omitted.keySet().containsAll(Lists.newArrayList("A", "B", "C", "D")));
    assertTrue(omitted.values().containsAll(Lists.newArrayList(1, 2, 3, 4)));
  }

  /**
   * Test omit with nulls.
   */
  @Test
  public void testOmitWithNulls() {
    final Map<String, Object> params = createMap();

    Map<String, Object> omitted = RequestUtils.omit(params);

    assertTrue(omitted.keySet().containsAll(Lists.newArrayList("A", "B", "C", "D")));
    assertTrue(omitted.values().containsAll(Lists.newArrayList(1, 2, 3, 4)));

    assertNull(RequestUtils.omit(null));
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
    
    Map<String, Object> omitted = RequestUtils.omit(params);

    assertTrue(omitted.keySet().containsAll(Lists.newArrayList("A", "B", "C", "D")));
    assertTrue(omitted.values().containsAll(Lists.newArrayList(1, 2, 3, 4)));

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
}
