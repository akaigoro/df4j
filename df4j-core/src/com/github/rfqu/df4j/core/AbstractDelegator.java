package com.github.rfqu.df4j.core;

/**
 * Communication part of decoupled actor (delegator).
 * @param <H> Computational part of decoupled actor (delegate).
 * @author kaigorodov
 */
public abstract class AbstractDelegator<M extends Link, H> extends Actor<M> {
    H handler;
    
    public AbstractDelegator() {
        setReady(false);
    }

    public void setHandler(H handler) {
        if (handler==null) {
            throw new IllegalArgumentException("handler is null");
        }
        synchronized (this) {
            if (this.handler!=null) {
                throw new IllegalStateException("handler is docked already");
            }
            this.handler = handler;
        }
        setReady(true);
    }
    
    /**
     * removes handler and as a result, stops message processing.
     * TODO make it async, as handler can be in use now.
     * @return
     */
    public H removeHandler() {
        H res;
        synchronized (this) {
            if (handler==null) {
                throw new IllegalStateException("handler is null");
            }
            res = handler;
            handler=null;
            setReady(false);
        }
        return res;
    }

    @Override
	protected void act(M message) throws Exception {
		act(message, handler);
	}

	@Override
	protected void complete() throws Exception {
		complete(handler);
	}

	protected abstract void act(M message, H handler) throws Exception;

    protected abstract void complete(H handler) throws Exception;
}
