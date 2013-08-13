package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.StreamPort;

public class CharChunkSource extends DataflowNode
    implements StreamPort<String>, Supply<CharChunk<?>>
{
	private StreamInput<CharChunk<?>> inp=new StreamInput<CharChunk<?>>();
	private StreamInput<StreamPort<CharChunk<?>>> demands=new StreamInput<StreamPort<CharChunk<?>>>();

	@Override
	public void demand(StreamPort<CharChunk<?>> port) {
		demands.post(port);
	}

	@Override
	public void post(String string) {
		StringChunk chunk=new StringChunk(null);
		chunk.setStr(string);
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

	@Override
	protected void act() {
		CharChunk<?> chunk=inp.get();
		StreamPort<CharChunk<?>> demand=demands.get();
		if (chunk==null) {
			demand.close();
		} else {
			demand.post(chunk);
		}
	}
}