package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.StreamPort;

public class ByteChunkSink implements StreamPort<ByteChunk<?>> {
	Supply<ByteChunk<?>> supply;
	ByteArrayChunk bac=new ByteArrayChunk(null, 1024);
    private byte[] res;
	
	public ByteChunkSink(Supply<ByteChunk<?>> supply) {
		this.supply = supply;
		supply.demand(this);
	}

	@Override
	public void post(ByteChunk<?> chunk) {
		ByteIterator it=chunk.byteIterator();
		while (it.hasNext()) {
			bac.add(it.next());
		}
		chunk.free();
		supply.demand(this);
	}

	@Override
	public void close() {
		res=bac.getBytes();
	}

	@Override
	public boolean isClosed() {
		return res!=null;
	}

	public byte[] getRes() {
		return res;
	}
	
}