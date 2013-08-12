package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.StreamPort;

class CharChunkSink implements StreamPort<CharChunk<?>> {
	Supply<CharChunk<?>> supply;
	StringBuilder sb=new StringBuilder();
	private String res;
	
	public CharChunkSink(Supply<CharChunk<?>> supply) {
		this.supply = supply;
		supply.demand(this);
	}

	@Override
	public void post(CharChunk<?> chunk) {
		CharIterator it=chunk.charIterator();
		while (it.hasNext()) {
			sb.append(it.next());
		}
		chunk.free();
		supply.demand(this);
	}

	@Override
	public void close() {
		res=sb.toString();
	}

	@Override
	public boolean isClosed() {
		return res!=null;
	}

	public String getRes() {
		return res;
	}
	
}