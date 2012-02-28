package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public class LiberalDelegator<H extends Delegate<Action<H>>> 
extends AbstractDelegator<Action<H>, H>{
    
    @Override
    protected void act(Action<H> message) throws Exception {
        message.act(handler.get());     
    }

    @Override
    protected void complete() throws Exception {
        // TODO Auto-generated method stub        
    }

}
