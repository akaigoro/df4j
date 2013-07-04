/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.concurrent.Future;


/**
 * A message that carries callback port and space for reply.
 * Similar to {@link CompletableFuture}, but listeners are of type
 *  {@link Port}<{@link Request}>, and so that listeners receive
 * the {@link Request} itself.
 * 
 * @param <T> actual type of Request (subclassed)
 * @param <R> type of the result value
 */
public class Request<T extends Request<T, R>, R>
   extends CompletableFutureBase<R, Port<T>>
   implements Callback<R>, Future<R>
{
    @SuppressWarnings("unchecked")
    @Override
    protected void broadcastResult(Port<T> listenerLoc) {
        listenerLoc.post((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void broadcastFailure(Port<T> listenerLoc) {
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

    public synchronized R getResult() {
        return value;
    }

    public synchronized Throwable getException() {
        return exc;
    }

}
