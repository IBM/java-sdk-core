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

package com.ibm.cloud.sdk.core.http;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RequestBody that takes an {@link InputStream}.
 *
 */
public class InputStreamRequestBody extends RequestBody {
  private static final Logger LOGGER = Logger.getLogger(InputStreamRequestBody.class.getName());

  private InputStream inputStream;
  private MediaType mediaType;
  private byte[] bytes;

  /**
   * Creates the @link {@link RequestBody} from an @link {@link InputStream}.
   *
   * @param mediaType the media type
   * @param inputStream the input stream
   * @return the request body
   */
  public static RequestBody create(final MediaType mediaType, final InputStream inputStream) {
    return new InputStreamRequestBody(inputStream, mediaType);
  }

  private InputStreamRequestBody(InputStream inputStream, MediaType mediaType) {
    this.inputStream = inputStream;
    this.mediaType = mediaType;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
      IOUtils.copy(inputStream, outputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.bytes = outputStream.toByteArray();
    try {
      outputStream.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not close inputStream byte array.", e);
      e.printStackTrace();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.squareup.okhttp.RequestBody#contentType()
   */
  @Override
  public MediaType contentType() {
    return mediaType;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.squareup.okhttp.RequestBody#writeTo(okio.BufferedSink)
   */
  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    Source source = null;

    try {
      if (bytes != null) {
        source = Okio.source(new ByteArrayInputStream(bytes));
      } else {
        source = Okio.source(inputStream);
      }
      sink.writeAll(source);
    } finally {
      Util.closeQuietly(source);
    }
  }
}
