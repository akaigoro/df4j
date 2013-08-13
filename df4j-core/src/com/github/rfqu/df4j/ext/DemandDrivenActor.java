/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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