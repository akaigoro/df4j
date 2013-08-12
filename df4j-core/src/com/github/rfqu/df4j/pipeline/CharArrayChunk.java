package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.Port;

public class CharArrayChunk extends CharChunk<CharArrayChunk> {
	private char[] array;
	private int capacity;
	private int len=0;

	public CharArrayChunk(Port<CharArrayChunk> returnPort, int capacity) {
		super(returnPort);
		this.capacity=capacity;
		this.array = new char[capacity];
	}
	
	public final void clear() {
		len=0;
	}
	
	public final boolean hasSpace() {
		return len<capacity;
	}

	public void add(char ch) {
		if (len==capacity) {
			throw new ArrayIndexOutOfBoundsException();
		}
		array[len++]=ch;
	}

	public CharIterator charIterator() {
		return new Source();
	}

	class Source implements CharIterator {
		int pos = 0;

		@Override
		public boolean hasNext() {
			return pos < len;
		}

		@Override
		public char next() {
			return array[pos++];
		}
	}
}