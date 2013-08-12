package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.Port;

public class StringChunk extends CharChunk<StringChunk> {
	protected String str;
	protected int len;
	
	public StringChunk(Port<StringChunk> returnPort) {
		super(returnPort);
	}
	
	public void setStr(String str) {
		this.str=str;
		this.len = str.length();
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
			return str.charAt(pos++);
		}
	}
}