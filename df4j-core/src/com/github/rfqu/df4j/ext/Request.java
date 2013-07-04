/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import java.util.concurrent.Future;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.CompletableFutureBase;
import com.github.rfqu.df4j.core.Port;

/**
 * A message that carries callback port.
 * Similar to {@link CompletableFuture}, but listeners are of type
 *  {@link Port}<{@link Request}>, and that listeners will receive
 * the request itself.
 * @param <T> actual type of Request (subclassed)
 * @param <R> type of result
 */
public class Request<T extends Request<T, R>, R>
   extends CompletableFutureBase<R, Port<T>>
   implements Callback<R>, Future<R>
{
    @SuppressWarnings("unchecked")
    @Override
    protected void informResult(Port<T> listenerLoc) {
        listenerLoc.post((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void informFailure(Port<T> listenerLoc) {
        listenerLoc.post((T) this);
    }

    public Request<T, R> addListener(Port<T> sink) {
        _addListener(sink);
        return this;
    }

    /**
     * reinitialize
     */
    public void reset() {
        _hasValue = false;
        value = null;
        exc = null;
        listener = null;
    }

    public void toCallback(Callback<R> handler) {
        if (exc == null) { // check exc, returned result may be null
            handler.post(value);
        } else {
            handler.postFailure(exc);
        }
    }

    public synchronized R getResult() {
        return value;
    }

    public synchronized Throwable getException() {
        return exc;
    }

}
