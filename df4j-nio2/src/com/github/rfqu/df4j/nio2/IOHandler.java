/* Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Actor;

/**
 * Handles the result of an IO operation.
 * Can be seen as a simplified actor, with space for only 1 incoming meassage.
 * @param <R> the type of accepted I/O requests.
 */
public abstract class IOHandler<R extends IORequest<R>>
  extends Actor<R>
{
    public IOHandler(Executor executor) {
        super(executor);
    }

    public IOHandler() {
    }

    /**
     * processes one incoming message
     * @param message the message to process
     * @throws Exception
     */
    protected void act(R request) throws Exception {
        Throwable exc = request.getExc();
        if (exc == null) {
            int result = request.getResult();
            if (result==-1) {
                closed(request);
            } else {
                completed(result, request);
            }
        } else {
            if (exc instanceof AsynchronousCloseException) {
                // System.out.println("  ServerRequest conn closed id="+id);
                closed(request);
            } else if (exc instanceof InterruptedByTimeoutException) {
                timedOut(request);
            } else {
                // System.out.println("  ServerRequest read failed id="+id+"; exc="+exc);
                failed(exc, request);
            }
        }
    }

    protected abstract void completed(int result, R request) throws Exception;

    protected void timedOut(R request) {
    }

    protected void closed(R request) throws Exception {
    }

    protected void failed(Throwable exc, R request) throws Exception {
    }
}
