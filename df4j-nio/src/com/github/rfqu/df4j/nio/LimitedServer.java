/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.ext.ImmediateExecutor;

/**
 * A server with limited number of connections
 * 
 * @author Alexei Kaigorodov
 */
public abstract class LimitedServer {
    protected AsyncServerSocketChannel assc;
    private ASCGenerator generator = new ASCGenerator();

    public void start(AsyncServerSocketChannel assc, SocketAddress addr, int waitCount, int maxCount) throws IOException {
		this.assc = assc;
        assc.bind(addr);
        generator.waitCount.up(waitCount);
        generator.maxCount.up(maxCount);
    }
    
    protected abstract void accepted(AsyncSocketChannel asc);

    public void close() {
        assc.close();
    }

    private class ASCGenerator extends DataflowNode {
        ConnSemafor waitCount = new ConnSemafor();
        CloseSemafor maxCount = new CloseSemafor();

        public ASCGenerator() {
            super(new ImmediateExecutor());
        }

        @Override
        protected void act() {
            ListenableFuture<AsyncSocketChannel> connEvent = assc.accept();
            connEvent.addListener(waitCount);
        }

        @Override
		protected void handleException(Throwable exc) {
            if (exc instanceof AsynchronousCloseException) {
            	// channel closed, just return without new call to accept()
            	return;
            }
        	super.handleException(exc);
		}

        class ConnSemafor extends Semafor implements Callback<AsyncSocketChannel> {

            @Override
            public void post(AsyncSocketChannel asc) {
                super.up();
                asc.getCloseEvent().addListener(maxCount);
                accepted(asc);
            }

            @Override
            public void postFailure(Throwable exc) {
                super.up();
                ASCGenerator.super.postFailure(exc);
            }
            
        }
        class CloseSemafor extends Semafor implements Callback<AsyncSocketChannel> {

            @Override
            public void post(AsyncSocketChannel asg) {
                super.up();
            }

            @Override
            public void postFailure(Throwable exc) {
                super.up();
                ASCGenerator.super.postFailure(exc);
            }
            
        }
    }
}
