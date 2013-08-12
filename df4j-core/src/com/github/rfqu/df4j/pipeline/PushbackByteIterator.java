package com.github.rfqu.df4j.pipeline;

public class PushbackByteIterator implements ByteIterator {
	ByteIterator it;

	public PushbackByteIterator(ByteIterator it) {
		this.it = it;
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte next() {
		// TODO Auto-generated method stub
		return 0;
	}

}
