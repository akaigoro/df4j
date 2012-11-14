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

import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.AbstractActor;
import com.github.rfqu.df4j.core.Callback;

public abstract class CallbackActor extends AbstractActor {
    protected Throwable exc=null;

    public CallbackActor(Executor executor) {
        super(executor);
    }

    public CallbackActor() {
    }
    
    public  void sendFailure(Throwable exc) {
        boolean doFire;
        synchronized(this) {
            if (this.exc!=null) {
                return; // only first failure is processed 
            }
            this.exc=exc;
            if (fired) {
                doFire=false;
            } else {
                doFire=fired=true;
            }
        }
        if (doFire) {
            fire();
        }
    }

    //========= backend
    
    protected abstract void act();

    protected abstract void handleException(Throwable exc);

    @Override
    protected void run() {
        try {
            for (;;) {
                synchronized (this) {
                    if (exc!=null) {
                        break; // fired remains true, preventing subsequent execution
                    }
                    if (!isReady()) {
                        fired = false; // allow firing
                        return;
                    }
                    consumeTokens();
                }
                act();
            }
        } catch (Throwable e) {
            exc=e;
        }
        try {
            handleException(exc);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    //====================== inner classes

    /** ScalarInput which also redirects failures 
     */
    public class CallbackInput<T> extends ScalarInput<T> implements Callback<T> {
        @Override
        public void sendFailure(Throwable exc) {
            CallbackActor.this.sendFailure(exc);
        }
    }
}