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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowVariable;

/**
 * Wrapper over {@link java.nio.channels.AsynchronousServerSocketChannel}.
 * Simplifies input-output, handling queues of accept requests.
 * @author rfqu
 *
 */
public class AsyncServerSocketChannel extends DataflowVariable
{
    private Input<Callback<AsynchronousSocketChannel>> acceptors=new StreamInput<Callback<AsynchronousSocketChannel>>();
    private Semafor pending=new Semafor();
    private Semafor maxConnLimit=new Semafor();
    private AsynchronousServerSocketChannel channel;
    
    public AsyncServerSocketChannel(InetSocketAddress addr, int maxConn)
                throws IOException
    {
        if (maxConn<=0) {
            throw new IllegalArgumentException("maxConn="+maxConn+"; should be positive");
        }
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
        maxConnLimit.up(maxConn);
        pending.up(); // allow accept
    }
    
	public void accept(Callback<AsynchronousSocketChannel> acceptor) {
		acceptors.send(acceptor);
	}

    public void upConnNumber() {
        maxConnLimit.up();
    }

    @Override
	protected void handleException(Throwable exc) {
        AsynchronousServerSocketChannel channelLoc;
        synchronized (this) {
            if (channel==null) {
                return;
            }
            channelLoc=channel;
            channel = null;
            for (Callback<?> acceptor: acceptors) {
            	acceptor.sendFailure(exc);
            }
        }
        try {
			channelLoc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

    public boolean isOpened() {
        return channel!=null;
    }

    //======================= backend
    AcceptHandler acceptHandler=new AcceptHandler();
    Callback<AsynchronousSocketChannel> consumer;

    @Override
    protected void act() {
    	consumer=acceptors.get();
        channel.accept(null, acceptHandler);
        // pending.off() and maxConnLimit.down() automatically by Pin' logic
    }
 
	class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
		/** new client connected */
		@Override
		public void completed(AsynchronousSocketChannel result, Void attachment) {
			consumer.send(result);
			pending.up();
		}

		/** new client connection failed */
		@Override
		public void failed(Throwable exc, Void attachment) {
			consumer.sendFailure(exc);
			if (exc instanceof AsynchronousCloseException) {
				// channel closed. sendFailure to all acceptors
				sendFailure(exc);
			} else {
				pending.up(); // continue accepting
			}
		}
	}
}
