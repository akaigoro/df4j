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
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Promise;

/**
 * Wrapper over {@link AsynchronousServerSocketChannel}.
 * Simplifies input-output, handling queues of accept requests.
 * 
 * Actual connection request acception occur only when both following conditions are met in any order:
 * <br>- client wants to connect  
 * <br>- server invokes {@link #accept(Callback<AsynchronousSocketChannel>) accept} </br> 
 * To allow more client connection, subsequent calls to  {link #accept(Callback<AsynchronousSocketChannel>) accept}
 * required. This way server can limit the number of accepted connections.   
 *  
 * <pre><code>AsyncServerSocketChannel assch=new AsyncServerSocketChannel(addr);
 * assch.accept(new Actor<AsynchronousSocketChannel>() {
 *    @Override
 *    public void act(AsynchronousSocketChannel achannel) {
 *    	AsyncSocketChannel channel=new AsyncSocketChannel(achannel);
 *      ....
 *      assch.accept(this); // allow one more client to connect
 *    }
 *  });
 * 
 * </code></pre>
 */
public class AsyncServerSocketChannel extends ActorVariable<Callback<AsynchronousSocketChannel>>
  implements CompletionHandler<AsynchronousSocketChannel, Callback<AsynchronousSocketChannel>>
{
    private final Semafor pending=new Semafor();
    private AsynchronousServerSocketChannel channel;
    private Promise<SocketAddress> closeEvent=new Promise<SocketAddress>();
    
    public AsyncServerSocketChannel(SocketAddress addr)
                throws IOException
    {
        if (addr==null) {
            throw new NullPointerException();
        }
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
        pending.up(); // allow accept
    }
    
    /** initiates acceptance process when the channel is free
     * @param acceptor
     */
	public void accept(Callback<AsynchronousSocketChannel> acceptor) {
		input.send(acceptor);
	}
	
    public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
    	closeEvent.addListener(listener);
        return listener;
    }
    
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

	//======================== Dataflow backend

    @Override
	protected void act(Callback<AsynchronousSocketChannel> consumer) {
        channel.accept(consumer, this);
        // pending.down() made automatically by Pin's logic
	}

	@Override
	protected void complete() throws Exception {
        SocketAddress addr = channel.getLocalAddress();
        try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        channel = null;
		closeEvent.send(addr);
	}

	@Override
	protected void handleException(Throwable exc) {
		close();  // eventually leads to complete() => channel.close();
        for (Callback<?> acceptor: super.input) {
        	acceptor.sendFailure(exc);
        }
    }
    
    //====================== CompletionHandler's backend
    
	/** new client connected */
	@Override
	public void completed(AsynchronousSocketChannel result, Callback<AsynchronousSocketChannel> consumer) {
		consumer.send(result);
		pending.up();
	}

	/** new client connection failed */
	@Override
	public void failed(Throwable exc, Callback<AsynchronousSocketChannel> consumer) {
		consumer.sendFailure(exc);
		if (exc instanceof AsynchronousCloseException) {
			// channel closed.
			// Call sendFailure to this channel and so to all acceptors
			sendFailure(exc);
		} else {
			 // only current acceptor receives this failure, continue accepting
			pending.up();
		}
	}
}
