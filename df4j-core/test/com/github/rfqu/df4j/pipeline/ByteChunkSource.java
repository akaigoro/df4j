package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.StreamPort;

public class ByteChunkSource extends DataflowNode
    implements StreamPort<byte[]>, Supply<ByteChunk<?>>
{
	private StreamInput<ByteArrayChunk> inp=new StreamInput<ByteArrayChunk>();
	private StreamInput<StreamPort<ByteChunk<?>>> demands=new StreamInput<StreamPort<ByteChunk<?>>>();

	@Override
	public void demand(StreamPort<ByteChunk<?>> port) {
		demands.post(port);
	}

	@Override
	public void post(byte[] data) {
        ByteArrayChunk chunk=new ByteArrayChunk(null, data);
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
		ByteChunk<?> chunk=inp.get();
		StreamPort<ByteChunk<?>> demand=demands.get();
		if (chunk==null) {
			demand.close();
		} else {
			demand.post(chunk);
		}
	}
}