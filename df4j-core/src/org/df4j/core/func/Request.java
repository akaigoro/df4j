/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.func;

import java.util.concurrent.Future;

import org.df4j.core.Listener;
import org.df4j.core.Port;


/**
 * A message that carries callback port and space for reply.
 * Similar to {@link Promise}, but listeners are of type
 *  {@link Port}<{@link Request}>, and so that listeners receive
 * the {@link Request} itself.
 * 
 * @param <T> actual type of Request (subclassed)
 * @param <R> type of the result value
 */
public class Request<T extends Request<T, R>, R>
   extends PromiseBase<R, Port<T>>
   implements Listener<R>, Future<R>
{
    @SuppressWarnings("unchecked")
    @Override
    protected void passResult(Port<T> listenerLoc) {
        listenerLoc.post((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void passFailure(Port<T> listenerLoc) {
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
