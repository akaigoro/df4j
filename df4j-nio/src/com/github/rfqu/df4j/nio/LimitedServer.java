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
import java.nio.channels.ClosedChannelException;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.ext.ImmediateExecutor;

/**
 * A server with limited number of connections
 * 
 * @author Alexei Kaigorodov
 */
public class LimitedServer {
    protected AsyncServerSocketChannel assc;
    private ASCGenerator generator = new ASCGenerator();
    private Port<AsyncSocketChannel> sink;

    public LimitedServer(AsyncServerSocketChannel assc) {
        this.assc = assc;
    }

    public LimitedServer(SocketAddress addr) throws IOException {
        this(AsyncChannelFactory.getCurrentAsyncChannelFactory()
             .newAsyncServerSocketChannel().bind(addr));
    }

    public void start(Port<AsyncSocketChannel> sink, int waitCount, int maxCount) {
        this.sink = sink;
        generator.waitCount.up(waitCount);
        generator.maxCount.up(maxCount);
    }
    
    public void close() {
        assc.close();
    }

    private class ASCGenerator extends DataflowNode {
        /** limits the number of accept requests */
        ConnSemafor waitCount = new ConnSemafor();
        /** limits the number of accepted connections */
        CloseSemafor maxCount = new CloseSemafor();

        public ASCGenerator() {
            super(new ImmediateExecutor());
        }

        /** invoked when both resource constraints are met */
        @Override
        protected void act() {
            try {
                // starts another acception
                ListenableFuture<AsyncSocketChannel> connEvent = assc.accept();
                // when connection accepted, increase waitcount
                connEvent.addListener(waitCount);
            } catch (ClosedChannelException e) {
            }
        }

        @Override
		protected void handleException(Throwable exc) {
            if ( (exc instanceof AsynchronousCloseException)
               ||(exc instanceof ClosedChannelException))
            {
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
                sink.post(asc);
            }

            @Override
            public void postFailure(Throwable exc) {
                super.up();
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
