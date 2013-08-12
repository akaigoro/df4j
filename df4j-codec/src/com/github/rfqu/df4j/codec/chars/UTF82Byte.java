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
import com.github.rfqu.df4j.pipeline.ByteArrayChunk;
import com.github.rfqu.df4j.pipeline.ByteChunk;
import com.github.rfqu.df4j.pipeline.CharChunk;
import com.github.rfqu.df4j.pipeline.CharIterator;
import com.github.rfqu.df4j.pipeline.Supply;

public class UTF82Byte extends DataflowNode 
	implements StreamPort<CharChunk<?>>, Supply<ByteChunk<?>>
{
	private Supply<CharChunk<?>> supply;
	private StreamInput<CharChunk<?>> inp = new StreamInput<CharChunk<?>>();
	private StreamInput<ByteArrayChunk> buffs = new StreamInput<ByteArrayChunk>();
	private StreamInput<StreamPort<ByteChunk<?>>> demands = new StreamInput<StreamPort<ByteChunk<?>>>();

	public UTF82Byte(Supply<CharChunk<?>> supply, int buffLen) {
		if (buffLen<3) {
			throw new IllegalArgumentException();
		}
		this.supply = supply;
		for (int k = 0; k < 2; k++) {
			ByteArrayChunk t = new ByteArrayChunk(buffs, buffLen);
			buffs.post(t);
		}
		supply.demand(this);
	}

	@Override
	public void demand(StreamPort<ByteChunk<?>> port) {
		demands.post(port);
	}

	@Override
	public void post(CharChunk<?> chunk) {
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

	CharIterator it = null;

	@Override
	protected void act() {
		CharChunk<?> inpChunk = inp.get();
		StreamPort<ByteChunk<?>> demand = demands.get();
		if (inpChunk == null) { // ==inp.isClosed();
			demand.close();
			return;
		}
		CharIterator it = this.it == null ? inpChunk.charIterator() : this.it;
		ByteArrayChunk outChunk = buffs.get();
		outChunk.clear();
		while (it.hasNext() && (outChunk.space()>=3)) {
			char ch = it.next();
	        if (ch<128) {
				outChunk.add((byte) ch);
	        } else if (ch<2048) { // 110x xxxx 10xx xxxx
	            byte b=(byte) (0xC0|(ch>>6));
	            outChunk.add(b);
	            b=(byte) (0x80|(ch&0x37));
	            outChunk.add(b);
	        } else { // 1110 xxxx 10xx xxxx 10xx xxxx
	            byte b=(byte) (0xE0|(ch>>12));
	            outChunk.add(b);
	            b=(byte) (0x80|((ch>>6)&0x37));
	            outChunk.add(b);
	            b=(byte) (0x80|(ch&0x37));
	            outChunk.add(b);
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
}
