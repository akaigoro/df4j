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

/**
 * abstract node with single output and exception handling
 * @param <R> type of result
 */
public abstract class Function<R> extends BaseActor {
    protected Throwable exc=null;
    protected boolean excpending=false;
	protected final Demand<R> res=new Demand<R>();

    public void addListener(Callback<R> sink) {
        res.addListener(sink);
    }
    
    public void addListeners(Callback<R>... sinks) {
        res.addListeners(sinks);
    }
    
    @Override
    public void run() {
        for (;;) {
            Throwable exc=null;
            synchronized (this) {
                if (this.exc != null) {
//                    System.err.println("run:"+exc);
                    exc=this.exc;
                    if (isReady()) {
//                        System.err.println("  run: isReady");
                        retrieveTokens();
                        this.exc=null;
                        if (excpending) {
                            excpending=false; // exc handling finished
                            continue;
                        }
                        excpending=false; // exc handling finished
                    } else {
                        excpending=true;  // to be continued
                    }
                } else if (!isReady()) {
//                    System.err.println("run:!isReady");
                    fired = false; // allow firing
                    return;
                } else {
//                    System.err.println("run: isReady");
                    retrieveTokens();
                }
            }
            if (exc==null) {
                act();
            } else {
                handleException(exc);
            }
        }
    }

   public void act() {
        try {
            res.send(eval());
        } catch (Exception e) {
            handleException(e);
        }
    }

    abstract protected R eval();

    protected void handleException(Throwable exc) {
        res.sendFailure(exc);
    }

    public class CallbackInput<T> extends ScalarInput<T> implements Callback<T> {
        @Override
        public void sendFailure(Throwable exc) {
            boolean doFire;
            synchronized (Function.this) {
                if (Function.this.exc==null) {
                    Function.this.exc=exc;
                    doFire=!fired;
                    fired=true;
                    filled=true;
                    turnOn();
                } else {
                    doFire=turnOn();
                }
            }
            if (doFire) {
                fire();
            }
        }
    }
}