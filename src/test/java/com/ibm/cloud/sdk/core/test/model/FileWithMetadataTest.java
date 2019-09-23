/**
 * (C) Copyright IBM Corp. 2019.
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

package com.ibm.cloud.sdk.core.test.model;

import com.ibm.cloud.sdk.core.service.model.FileWithMetadata;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * A few simple tests that exercise the FileWithMetadata model.
 */
public class FileWithMetadataTest {

    @Test
    public void testBuilderWithOnlyRequired() {
        byte[] fileBytes = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        InputStream inputStream = new ByteArrayInputStream(fileBytes);
        FileWithMetadata.Builder builder = new FileWithMetadata.Builder();
        builder.data(inputStream);
        FileWithMetadata fileWithMetadata = builder.build();
        assertNotNull(fileWithMetadata);
        assertNotNull(fileWithMetadata.data());
        assertNull(fileWithMetadata.filename());
        assertNull(fileWithMetadata.contentType());
    }

    @Test
    public void testBuilderWithFile() throws FileNotFoundException {
        String filename = "my-credentials.env";
        final File myFile = new File("src/test/resources/" + filename);
        FileWithMetadata.Builder builder = new FileWithMetadata.Builder();
        builder.data(myFile);
        FileWithMetadata fileWithMetadata = builder.build();
        assertNotNull(fileWithMetadata);
        assertNotNull(fileWithMetadata.data());
        assertEquals(filename, fileWithMetadata.filename());
        assertNull(fileWithMetadata.contentType());
    }

    @Test
    public void testBuilderWithAllProps() {
        byte[] fileBytes = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe};
        String filename = "my-awesome-file.txt";
        String contentType = "text/plain";
        InputStream inputStream = new ByteArrayInputStream(fileBytes);
        FileWithMetadata.Builder builder = new FileWithMetadata.Builder(inputStream);
        builder.filename(filename);
        builder.contentType(contentType);
        FileWithMetadata fileWithMetadata = builder.build();
        assertNotNull(fileWithMetadata);
        assertNotNull(fileWithMetadata.data());
        assertEquals(filename, fileWithMetadata.filename());
        assertEquals(contentType, fileWithMetadata.contentType());
    }

    @Test
    public void testBuilderConstructorWithFile() throws FileNotFoundException {
        String filename = "my-credentials.env";
        final File myFile = new File("src/test/resources/" + filename);
        FileWithMetadata fileWithMetadata = new FileWithMetadata.Builder(myFile).build();
        assertNotNull(fileWithMetadata);
        assertNotNull(fileWithMetadata.data());
        assertEquals(filename, fileWithMetadata.filename());
        assertNull(fileWithMetadata.contentType());
    }

    @Test
    public void testNewBuilder() {
        byte[] fileBytes = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe};
        String filename = "my-awesome-file.txt";
        String contentType = "text/plain";
        FileWithMetadata orig = new FileWithMetadata.Builder()
                .data(new ByteArrayInputStream(fileBytes))
                .filename("foo.txt")
                .contentType("text/plain")
                .build();
        FileWithMetadata.Builder builder = orig.newBuilder();
        builder.filename(filename);
        builder.contentType(contentType);
        FileWithMetadata fileWithMetadata = builder.build();
        assertNotNull(fileWithMetadata);
        assertNotNull(fileWithMetadata.data());
        assertEquals(filename, fileWithMetadata.filename());
        assertEquals(contentType, fileWithMetadata.contentType());
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testValidation() {
        FileWithMetadata.Builder builder = new FileWithMetadata.Builder();
        FileWithMetadata fileWithMetadata = builder.build();
    }
}
