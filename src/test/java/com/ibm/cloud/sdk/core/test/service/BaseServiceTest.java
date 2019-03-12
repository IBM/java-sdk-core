/**
 * Copyright 2018 IBM Corp. All Rights Reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.cloud.sdk.core.service.BaseService;

/**
 * Unit tests associated with the BaseService core class.
 *
 */
public class BaseServiceTest {

  @Test
  public void testMimeTypes() {
    assertTrue(BaseService.isJsonMimeType("application/json"));
    assertTrue(BaseService.isJsonMimeType("application/json; charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("application/json;charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("APPLICATION/JSON;charset=utf-16"));
    assertFalse(BaseService.isJsonMimeType("application/notjson"));
    assertFalse(BaseService.isJsonMimeType("application/json-patch+json"));
    assertFalse(BaseService.isJsonMimeType("APPlication/JSON-patCH+jSoN;charset=utf-8"));
    assertTrue(BaseService.isJsonPatchMimeType("APPlication/JSON-patCH+jSoN;charset=utf-8"));
    assertTrue(BaseService.isJsonMimeType("application/merge-patch+json"));
    assertTrue(BaseService.isJsonMimeType("application/merge-patch+json;charset=utf-8"));
    assertFalse(BaseService.isJsonMimeType("application/json2-patch+json"));
    assertFalse(BaseService.isJsonMimeType("application/merge-patch+json-blah"));
    assertFalse(BaseService.isJsonMimeType("application/merge patch json"));

    assertTrue(BaseService.isJsonPatchMimeType("application/json-patch+json"));
    assertTrue(BaseService.isJsonPatchMimeType("application/json-patch+json;charset=utf-8"));
    assertFalse(BaseService.isJsonPatchMimeType("application/json"));
    assertFalse(BaseService.isJsonPatchMimeType("APPLICATION/JsOn; charset=utf-8"));
    assertFalse(BaseService.isJsonPatchMimeType("application/merge-patch+json"));
    assertFalse(BaseService.isJsonPatchMimeType("application/merge-patch+json;charset=utf-8"));
  }
}
