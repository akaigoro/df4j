package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.Port;

public abstract class ByteChunk<C extends ByteChunk<C>> extends ChunkBase<C>
    implements ByteIterable
{
	
	public ByteChunk(Port<C> returnPort) {
		super(returnPort);
	}

}