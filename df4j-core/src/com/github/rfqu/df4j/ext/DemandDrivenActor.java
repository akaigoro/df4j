package com.github.rfqu.df4j.ext;

import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.Promise;

/**
 * Demonstrates declaration of custom {@link Pin} 
 */
public abstract class DemandDrivenActor<T> extends Actor<T> {
    public DemandDrivenActor() {
    }

    public DemandDrivenActor(Executor executor) {
        super(executor);
    }

    /**
     * This pin carries demand(s) of the result.
     * Demand is two-fold: it is an input pin, so firing possible only if
     * someone demanded the execution, and it holds listeners' ports where
     * the result should be sent. 
     * @param <R>  type of result
     */
    public class Demand<R> extends PinBase<Callback<R>> implements Promise<R>, Callback<R> {
        private CompletableFuture<R> listeners=new CompletableFuture<R>();

        /** indicates a demand
         * @param sink Port to send the result
         * @return 
         */
        @Override
        public Promise<R> addListener(Callback<R> sink) {
            checkOn(sink);
            return this;
        }

        @Override
        protected boolean turnedOn(Callback<R> sink) {
            listeners.addListener(sink);
            return true;
        }

        /** satisfy demand(s)
         */
        @Override
        public void post(R m) {
            listeners.post(m);
        }

        @Override
        public void postFailure(Throwable exc) {
            listeners.postFailure(exc);
        }
    }

}