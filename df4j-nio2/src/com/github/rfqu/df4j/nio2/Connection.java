/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.InterruptedByTimeoutException;
import com.github.rfqu.df4j.core.MultiPortActor;

/**
 * Container for I/O handlers.
 * Handling procedures are executed from within
 * the {@link java.nio.channels.CompletionHandler}s.
 * Simultaneous {@link java.nio.channels.CompletionHandler} requests
 * to the handling procedures are queued, so no additional synchronization is needed.
 * @author Alexei Kaigorodov
 */
public class Connection extends MultiPortActor {

    /**
     * Sets null Executor, in order to make handling immediately from
     *  {@link java.nio.channels.CompletionHandler}s.
     */
    public Connection() {
        super(null);
    }
    
    protected abstract class IOHandler<R extends IORequest<R>> extends PortHandler<R> {
      /**
       * processes one incoming message
       * @param request the request to process
       */
      protected void act(R request) {
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

      protected abstract void completed(int result, R request);// throws Exception;

      protected void timedOut(R request) {
      }

      protected void closed(R request) {// throws Exception {
      }

      protected void failed(Throwable exc, R request) {// throws Exception {
      }
  }
    
    protected abstract class FileIOHandler<R extends FileIORequest<R>> extends IOHandler<R> {        
    }
    
    protected abstract class SocketIOHandler<R extends SocketIORequest<R>> extends IOHandler<R> {
    }
}
