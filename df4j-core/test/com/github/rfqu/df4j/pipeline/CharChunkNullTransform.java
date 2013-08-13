package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.StreamPort;

class CharChunkNullTransform extends DataflowNode
    implements StreamPort<CharChunk<?>>, Supply<CharChunk<?>>
{
	private Supply<CharChunk<?>> supply;
	private StreamInput<CharChunk<?>> inp=new StreamInput<CharChunk<?>>();
	private StreamInput<CharArrayChunk> buffs=new StreamInput<CharArrayChunk>();
	private StreamInput<StreamPort<CharChunk<?>>> demands=new StreamInput<StreamPort<CharChunk<?>>>();

	public CharChunkNullTransform(Supply<CharChunk<?>> supply, int buffLen) {
		this.supply = supply;
		for (int k=0; k<2; k++) {
			CharArrayChunk t=new CharArrayChunk(buffs, buffLen);
			buffs.post(t);
		}
		supply.demand(this);
	}

	@Override
	public void demand(StreamPort<CharChunk<?>> port) {
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
	
	CharIterator it=null;

	@Override
	protected void act() {
		CharChunk<?> inpChunk=inp.get();
		StreamPort<CharChunk<?>> demand=demands.get();
		if (inpChunk==null) { // ==inp.isClosed();
			demand.close();
			return;
		}
		CharIterator it = this.it==null? inpChunk.charIterator():this.it;
		CharArrayChunk outChunk=buffs.get();
		outChunk.clear();
		while (it.hasNext() && outChunk.hasSpace()) {
			char ch=it.next();
			outChunk.add(ch);
		}
		if (it.hasNext()) {
			inp.pushback();
			this.it=it;
		} else {
			inpChunk.free();
			supply.demand(this);
			this.it=null;
		}
		demand.post(outChunk);
	}
}