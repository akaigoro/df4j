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
	public void demand(StreamPort<CharChunk<?>> consumer) {
		if (closed) {
			consumer.close();
		} else {
			demands.post(consumer);
		}
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
	private volatile boolean closed;

	@SuppressWarnings("resource")
	@Override
	protected void act() {
		CharChunk<?> inpChunk=inp.get();
		StreamPort<CharChunk<?>> demand=demands.get();
		if (inpChunk==null) { // ==inp.isClosed();
			closed=true;
			demand.close();
			return;
		}
		if (it==null) {
			it=inpChunk.charIterator();
		}
		CharArrayChunk outChunk=buffs.get();
		outChunk.clear();
		for (;;) {
			while (it.hasNext() && outChunk.hasSpace()) {
				char ch=it.next();
				outChunk.add(ch);
			}
			if (!it.hasNext()) {
				inpChunk.free();
				it=null;
				inpChunk=null;
				switch (inp.hasNext()) {
				case -1: // EOF
					closed=true;
					inp.moveNext();
					demand.post(outChunk);
					demand.close();
					// TODO free buffers
					return;
				case 0: 
					demand.post(outChunk);
					return;
				}
				inpChunk=inp.moveNext();
				supply.demand(this);
				it=inpChunk.charIterator();
			}
			if (!outChunk.hasSpace()) {
				demand.post(outChunk);
				if ((buffs.hasNext()==0) || (demands.hasNext()==0)) {
					if (it.hasNext()) {
						inp.pushback();
					} else {
						it=null;
						inpChunk.free();
					}
					return;
				}
				demand=demands.moveNext();
				outChunk=buffs.moveNext();
				outChunk.clear();
			}
		}
	}
	
	protected void act1() {
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