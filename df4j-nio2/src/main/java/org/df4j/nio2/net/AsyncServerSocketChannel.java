/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.nio2.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.stream.Stream;
import org.df4j.core.*;
import org.df4j.pipeline.ReactiveActor;

/**
 * Accepts incoming connections, wraps them in {@link AsyncSocketChannel},
 * and sends to the next node in the pipeline.
 * For using on server side.
 * <pre><code>AsyncServerSocketChannel assc =
 *     new AsyncServerSocketChannel(socketAddress, connNumber);
 * PipeLine pipeline = new PipeLine();
 * pipeline.setSource(assc)...
 * </code></pre>
 */
public class AsyncServerSocketChannel extends ReactiveActor
    implements CompletionHandler<AsynchronousSocketChannel, Void>
{
    protected volatile AsynchronousServerSocketChannel assc;
    
    /** prevents simultaneous channel.accept() */
    protected Semafor channelAccess = new Semafor();
    
    /** 
     * limits the number of active connections.
     * Used {@link AsyncSocketChannel}s return here.
     * They are not reused, just counted by the connCount Semafore.
     */
    protected ReactiveSemStreamOutput<AsyncSocketChannel> output = new ReactiveSemStreamOutput<>();
    
    public AsyncServerSocketChannel(SocketAddress addr, int connNumber) throws IOException {
        if (addr==null) {
            throw new NullPointerException();
        }
        if (connNumber<=0) {
            throw new IllegalArgumentException("connNumber must be positive");
        }
        assc=AsynchronousServerSocketChannel.open();
        assc.bind(addr);
        output.request(connNumber);
        channelAccess.up();
    }

    public synchronized void close() throws IOException {
    	if (assc==null) {
    		return;
    	}
    	AsynchronousServerSocketChannel asscLock=assc;
        assc = null;
        asscLock.close();
    }

    //====================== Dataflow backend

    @Override
    protected void act() {
        assc.accept(null, this);
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, Void attachment) {
        try {
            AsyncSocketChannel asc=new AsyncSocketChannel(result, output);
            output.post(asc);
            channelAccess.up(); // allow assc.accpt()
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** new client connection failed
     * TODO count failures, do not retry if many 
     */
    @Override
    public void failed(Throwable exc, Void attachment) {
        if (exc instanceof AsynchronousCloseException) {
            // channel closed.
            close();
        } else {
            channelAccess.up();
        }
    }
}
