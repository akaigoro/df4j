package com.github.rfqu.df4j.pipeline;

import com.github.rfqu.df4j.core.StreamPort;

public interface Supply<T> {
	public void demand(StreamPort<T> port);
}
