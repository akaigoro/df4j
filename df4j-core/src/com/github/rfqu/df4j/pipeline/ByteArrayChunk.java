package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.Port;

public class ByteArrayChunk extends ByteChunk<ByteArrayChunk> {
	protected byte[] array;
	protected int len;
	private int capacity;

	public ByteArrayChunk(Port<ByteArrayChunk> returnPort, int capacity) {
		super(returnPort);
		this.capacity=capacity;
		array=new byte[capacity];
	}
	
	public void setArray(byte[] array) {
		this.array = array;
		this.len = this.capacity=array.length;
	}		

	public final void clear() {
		len=0;
	}
	
	public final int space() {
		return capacity-len;
	}

	public final boolean hasSpace() {
		return len<capacity;
	}

	public void add(byte b) {
		if (len==capacity) {
			throw new ArrayIndexOutOfBoundsException();
		}
		array[len++]=b;
	}

	public ByteIterator byteIterator() {
		return new Source();
	}

	class Source implements ByteIterator {
		int pos = 0;

		@Override
		public boolean hasNext() {
			return pos < len;
		}

		@Override
		public byte next() {
			return array[pos++];
		}
	}
}