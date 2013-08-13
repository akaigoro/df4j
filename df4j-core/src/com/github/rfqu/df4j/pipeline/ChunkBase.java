package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.Port;

public class ChunkBase<C extends ChunkBase<C>> {
//	protected Port<Chunk<C>> returnPort;

	protected Port<C> returnPort;

	public ChunkBase(Port<C> returnPort) {
		this.returnPort = returnPort;
	}
	
	@SuppressWarnings("unchecked")
	public void free() {
		if (returnPort==null) {
			return;
		}
		returnPort.post((C) this);
	}

}
