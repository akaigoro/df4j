package com.github.rfqu.df4j.actordemux;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

abstract class AbstractDelegator<T, M extends Link, H> extends Actor<M> {
    T tag;
    
    public AbstractDelegator(T tag) {
        this.tag=tag;
    }

    protected final ScalarInput handler = new ScalarInput();

    /**
     * A place for single token loaded with a reference of type <T>
     * 
     * @param <T>
     */
    public class ScalarInput extends Pin implements Port<H> {
        public H token = null;
        protected boolean filled = false;

        @Override
        public void send(H newToken) {
            boolean doFire;
            synchronized (AbstractDelegator.this) {
                if (filled) {
                    throw new IllegalStateException("place is occupied already");
                }
                token = newToken;
                filled = true;
                doFire = turnOn();
            }
            if (doFire) {
                fire();
            }
        }
    }
}