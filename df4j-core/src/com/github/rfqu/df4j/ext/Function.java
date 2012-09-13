/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
//package com.github.rfqu.df4j.util;
package com.github.rfqu.df4j.ext;

import com.github.rfqu.df4j.core.BaseActor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataSource;

/**
 * abstract node with multiple inputs, single output and exception handling
 * Unlike Actor, it is single shot. 
 * @param <R> type of result
 */
public abstract class Function<R> extends BaseActor implements DataSource<R> {
    protected Throwable exc=null;
	protected final Demand<R> res=new Demand<R>();
    protected boolean shot=false;

    /**
     * Subscribes a consumer to which the result will be send.
     * Function evaluation would not start until at least one
     * consumer subscribes.
     * It is allowed to subscribe after the function is evaluated.
     * @param sink
     * @return 
     */
    @Override
    public Function<R> addListener(Callback<R> sink) {
        res.addListener(sink);
        return this;
    }
    
    public void run() {
       if (exc==null) {
           try {
               res.send(eval());
           } catch (Exception e) {
               handleException(e);
           }
       } else {
           handleException(exc);
       }
    }

    /**
     * evaluates the function's result
     * @return
     */
    abstract protected R eval();

    protected void handleException(Throwable exc) {
        res.sendFailure(exc);
    }

    /** A place for single token loaded with a reference of type <T>
     * @param <T> 
     */
    public class CallbackInput<T> extends Pin implements Callback<T> {
        public T value=null;
        protected boolean filled=false;

        @Override
        public void send(T newToken) {
            boolean doFire;
            synchronized (Function.this) {
                if (filled) {
                    throw new IllegalStateException("place is occupied already"); 
                }
                if (shot) {
                    return;
                }
                value=newToken;
                filled=true;
                shot=doFire=turnOn();
            }
            if (doFire) {
                fire();
            }
        }

        @Override
        public void sendFailure(Throwable exc) {
            synchronized (Function.this) {
                if (filled) {
                    throw new IllegalStateException("place is occupied already"); 
                }
                if (shot) {
                    return;
                }
                Function.this.exc=exc;
                fired=filled=true;
                turnOn();
            }
            fire();
        }
    }
}