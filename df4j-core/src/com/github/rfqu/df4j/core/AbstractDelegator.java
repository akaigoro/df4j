package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public abstract class AbstractDelegator<M extends Link, H> extends Actor<M> {
    ScalarInput<H> handler=new ScalarInput<H>();
    {start();}
    
    public void setHandler(H handler) {
    	this.handler.send(handler);
    }
    
    /**
     * removes handler and as a result, stops message processing.
     * TODO make it async, as handler can be in use now.
     * @return
     */
    public H removeHandler() {
        return this.handler.remove();
    }
}
