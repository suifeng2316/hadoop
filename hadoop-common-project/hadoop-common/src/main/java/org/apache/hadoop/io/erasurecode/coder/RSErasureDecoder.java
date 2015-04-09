/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.io.erasurecode.coder;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.io.erasurecode.ECBlock;
import org.apache.hadoop.io.erasurecode.ECBlockGroup;
import org.apache.hadoop.io.erasurecode.rawcoder.RSRawDecoder;
import org.apache.hadoop.io.erasurecode.rawcoder.RawErasureDecoder;
import org.apache.hadoop.io.erasurecode.rawcoder.XORRawDecoder;

/**
 * Reed-Solomon erasure decoder that decodes a block group.
 *
 * It implements {@link ErasureCoder}.
 */
public class RSErasureDecoder extends AbstractErasureDecoder {
  private RawErasureDecoder rsRawDecoder;
  private RawErasureDecoder xorRawDecoder;
  private boolean useXorWhenPossible = true;

  @Override
  public void setConf(Configuration conf) {
    super.setConf(conf);

    if (conf != null) {
      this.useXorWhenPossible = conf.getBoolean(
          CommonConfigurationKeys.IO_ERASURECODE_CODEC_RS_USEXOR_KEY, true);
    }
  }

    @Override
  protected ErasureCodingStep prepareDecodingStep(final ECBlockGroup blockGroup) {

    RawErasureDecoder rawDecoder;

    ECBlock[] inputBlocks = getInputBlocks(blockGroup);
    ECBlock[] outputBlocks = getOutputBlocks(blockGroup);

    /**
     * Optimization: according to some benchmark, when only one block is erased
     * and to be recovering, the most simple XOR scheme can be much efficient.
     * We will have benchmark tests to verify this opt is effect or not.
     */
    if (outputBlocks.length == 1 && useXorWhenPossible) {
      rawDecoder = checkCreateXorRawDecoder();
    } else {
      rawDecoder = checkCreateRSRawDecoder();
    }

    return new ErasureDecodingStep(inputBlocks,
        getErasedIndexes(inputBlocks), outputBlocks, rawDecoder);
  }

  private RawErasureDecoder checkCreateRSRawDecoder() {
    if (rsRawDecoder == null) {
      rsRawDecoder = createRawDecoder(
          CommonConfigurationKeys.IO_ERASURECODE_CODEC_RS_RAWCODER_KEY);
      if (rsRawDecoder == null) {
        rsRawDecoder = new RSRawDecoder();
      }
      rsRawDecoder.initialize(getNumDataUnits(),
          getNumParityUnits(), getChunkSize());
    }
    return rsRawDecoder;
  }

  private RawErasureDecoder checkCreateXorRawDecoder() {
    if (xorRawDecoder == null) {
      xorRawDecoder = new XORRawDecoder();
      xorRawDecoder.initialize(getNumDataUnits(), 1, getChunkSize());
    }
    return xorRawDecoder;
  }

  @Override
  public void release() {
    if (xorRawDecoder != null) {
      xorRawDecoder.release();
    } else if (rsRawDecoder != null) {
      rsRawDecoder.release();
    }
  }
}
