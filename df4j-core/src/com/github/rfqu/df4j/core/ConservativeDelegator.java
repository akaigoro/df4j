package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public class ConservativeDelegator<M extends Link, H extends Delegate<M>>
extends AbstractDelegator<M, H> {

    @Override
    protected void act(M message) {
        _handler.act(message);
    }
    
	@Override
	protected void complete() throws Exception {
		_handler.complete();
	}
}
