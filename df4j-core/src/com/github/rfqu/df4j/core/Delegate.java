package com.github.rfqu.df4j.core;

public interface Delegate<M extends Link> {
	public void act(M message);
	public void complete();
}
