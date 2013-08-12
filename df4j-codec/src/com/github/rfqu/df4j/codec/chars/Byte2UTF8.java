/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.codec.chars;

import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.pipeline.CharArrayChunk;
import com.github.rfqu.df4j.pipeline.ByteChunk;
import com.github.rfqu.df4j.pipeline.CharChunk;
import com.github.rfqu.df4j.pipeline.ByteIterator;
import com.github.rfqu.df4j.pipeline.Supply;

public class Byte2UTF8 extends DataflowNode 
	implements StreamPort<ByteChunk<?>>, Supply<CharChunk<?>>
{
	private Supply<ByteChunk<?>> supply;
	private StreamInput<ByteChunk<?>> inp = new StreamInput<ByteChunk<?>>();
	private StreamInput<CharArrayChunk> buffs = new StreamInput<CharArrayChunk>();
	private StreamInput<StreamPort<CharChunk<?>>> demands = new StreamInput<StreamPort<CharChunk<?>>>();

	public Byte2UTF8(Supply<ByteChunk<?>> supply, int buffLen) {
		if (buffLen<3) {
			throw new IllegalArgumentException();
		}
		this.supply = supply;
		for (int k = 0; k < 2; k++) {
			CharArrayChunk t = new CharArrayChunk(buffs, buffLen);
			buffs.post(t);
		}
		supply.demand(this);
	}

	@Override
	public void demand(StreamPort<CharChunk<?>> port) {
		demands.post(port);
	}

	@Override
	public void post(ByteChunk<?> chunk) {
		inp.post(chunk);
	}

	@Override
	public void close() {
		inp.close();
	}

	@Override
	public boolean isClosed() {
		return inp.isClosed();
	}

	ByteIterator it = null;

	@Override
	protected void act() {
		ByteChunk<?> inpChunk = inp.get();
		StreamPort<CharChunk<?>> demand = demands.get();
		if (inpChunk == null) { // ==inp.isClosed();
			demand.close();
			return;
		}
		ByteIterator it = this.it == null ? inpChunk.byteIterator() : this.it;
		CharArrayChunk outChunk = buffs.get();
		outChunk.clear();
		while (it.hasNext() && outChunk.hasSpace()) {
			byte b0 = it.next();
	        if (b0<128) {
				outChunk.add((char) b0);
	        } else {
	        	int numBytes=zeroPos(b0)-2; // extra bytes required
				if (!it.has(numBytes)) {
					it.pushback(b0);
					break;
				}
	        	int ch;
				if (numBytes==1) { // 110x xxxx 10xx xxxx
		            byte b1=it.next();
		            ch=(b0&0x1F<<6) | (b1&0x3F);
		        } else if (numBytes==2) { // 1110 xxxx 10xx xxxx 10xx xxxx
		            byte b1=it.next();
		            byte b2=it.next();
		            ch=(b0&0x0F<<12) |(b1&0x3F<<8) | (b2&0x3F);
		        } else {
		        	// TODO report error
		        }
				outChunk.add((char) ch);
	        }
		}
		if (it.hasNext()) {
			inp.pushback();
			this.it = it;
		} else {
			inpChunk.free();
			this.it = null;
			supply.demand(this);
		}
		demand.post(outChunk);
	}

	/** counted from left to right, started from 1
     * @param b
     * @return position of zero bit
     */
    static int zeroPos(int b) {
        for (int k = 1; k <7; k++) {
            int mask = 0x100 >> k;
            if ((b & mask) == 0) {
                return k;
            }
        }
        return 7;
    }
}