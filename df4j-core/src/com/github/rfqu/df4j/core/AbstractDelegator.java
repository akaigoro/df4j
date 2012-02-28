package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public abstract class AbstractDelegator<M extends Link, H> extends Actor<M> {
    public final ScalarInput<H> handler=new ScalarInput<H>();
    protected H _handler;

    @Override
    protected void removeTokens() {
        super.removeTokens(); // remove message
        _handler=handler.get();
    }
}
