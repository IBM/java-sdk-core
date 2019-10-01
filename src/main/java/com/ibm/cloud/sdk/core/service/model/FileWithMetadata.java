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

package com.ibm.cloud.sdk.core.service.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A file and its associated metadata.
 */
public class FileWithMetadata {

    private InputStream data;
    private String filename;
    private String contentType;

    /**
     * Builder.
     */
    public static class Builder {
        private InputStream data;
        private String filename;
        private String contentType;

        private Builder(FileWithMetadata fileWithMetadata) {
            this.data = fileWithMetadata.data;
            this.filename = fileWithMetadata.filename;
            this.contentType = fileWithMetadata.contentType;
        }

        /**
         * Instantiates a new builder.
         */
        public Builder() {
        }

        /**
         * Instantiates a new builder with required properties.
         *
         * @param data the data / contents of the file
         */
        public Builder(InputStream data) {
            this.data = data;
        }

        /**
         * Instantiates a new builder with required properties.
         *
         * @param file the file to use as the source of file contents and filename
         *
         * @throws FileNotFoundException if the file could not be found
         */
        public Builder(File file) throws FileNotFoundException {
            this.data(file);
        }

        /**
         * Builds a FileWithMetadata.
         *
         * @return the fileWithMetadata
         */
        public FileWithMetadata build() {
            return new FileWithMetadata(this);
        }

        /**
         * Set the data.
         *
         * @param data the data
         * @return the FileWithMetadata builder
         */
        public Builder data(InputStream data) {
            this.data = data;
            return this;
        }

        /**
         * Set the filename.
         *
         * @param filename the filename
         * @return the FileWithMetadata builder
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Set the contentType.
         *
         * @param contentType the contentType
         * @return the FileWithMetadata builder
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Set the data.
         *
         * @param file the file to use as the source of file contents and filename
         * @return the FileWithMetadata builder
         * @throws FileNotFoundException if the file could not be found
         */
        public Builder data(File file) throws FileNotFoundException {
            this.data = new FileInputStream(file);
            this.filename = file.getName();
            return this;
        }
    }

    private FileWithMetadata(Builder builder) {
        com.ibm.cloud.sdk.core.util.Validator.notNull(builder.data,
                "data cannot be null");
        data = builder.data;
        filename = builder.filename;
        contentType = builder.contentType;
    }

    /**
     * New builder.
     *
     * @return a FileWithMetadata builder
     */
    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * The data / contents of the file.
     * @return the contents of the file
     */
    public InputStream data() {
        return this.data;
    }

    /**
     * The filename for file.
     * @return the filename
     */
    public String filename() {
        return this.filename;
    };

    /**
     * The content type of file. Values for this parameter can be obtained from the HttpMediaType class.
     * @return the content-type associated with the file
     */
    public String contentType() {
        return this.contentType;
    };

}
