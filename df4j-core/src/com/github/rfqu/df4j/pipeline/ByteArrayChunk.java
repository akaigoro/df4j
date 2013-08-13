package com.github.rfqu.df4j.pipeline;

import java.util.Arrays;

import com.github.rfqu.df4j.core.Port;

public class ByteArrayChunk extends ByteChunk<ByteArrayChunk> {
	protected byte[] array;
	protected int len;
	private int capacity;

    /** creates empty chunk */
	public ByteArrayChunk(Port<ByteArrayChunk> returnPort, int capacity) {
        super(returnPort);
        this.array=new byte[capacity];
        this.capacity=array.length;
	}
	
	/** creates full chunk */
	public ByteArrayChunk(Port<ByteArrayChunk> returnPort, byte[] data) {
        super(returnPort);
        this.array=data;
        this.capacity=this.len=data.length;
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

    public byte[] getBytes() {
        return Arrays.copyOf(array, len);
    }
}