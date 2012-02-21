package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public class LiberalDelegator<H> extends AbstractDelegator<Action<H>, H> {

	@Override
	protected void act(Action<H> message, H handler) throws Exception {
		message.act(handler);
	}

	@Override
	protected void complete(H handler) throws Exception {
		
	}
}
