/*
 * Copyright 2011 by Alexei Kaigorodov
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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackPromise;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

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
public class AsyncServerSocketChannel2 extends AsyncServerSocketChannel
  implements CompletionHandler<AsynchronousSocketChannel, Callback<AsyncSocketChannel>>
{
    private SocketAddress addr;
    private Callback<AsyncSocketChannel> acceptor;
    /** how many connections may be accepted */
    private int maxConn = 0;
    private AsynchronousServerSocketChannel channel;
    private CallbackPromise<SocketAddress> closeEvent=new CallbackPromise<SocketAddress>();
    
    public AsyncServerSocketChannel2(SocketAddress addr, Callback<AsyncSocketChannel> acceptor)
                throws IOException
    {
        super(addr, acceptor);
        if (addr==null) {
            throw new NullPointerException();
        }
        this.addr=addr;
        this.acceptor=acceptor;
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
    }
    
    /** initiates acceptance process. Beware of  when the channel is free
     * @param acceptor
     *        port to receive opened connection
     * @throws AsynchronousCloseException 
     * @throws  AcceptPendingException
     *          If an accept operation is already in progress on this channel
     * @throws  NotYetBoundException
     *          If this channel's socket has not yet been bound
     * @throws  ShutdownChannelGroupException
     *          If the channel group has terminated
     */
    /**
     * initiates acceptance process when the channel is free
     * 
     * @param acceptor
     */
    public synchronized void up(int delta) {
        if (delta<0) {
            throw new IllegalArgumentException();
        }
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (maxConn>0) { // there is waiting server-side connection
            maxConn+=delta;
            // do nothing: already registered at selector
            return;
        }
        maxConn=delta;
        channel.accept(acceptor, this);
    }
	
    public void up() {
        up(1);
    }

    public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
    	closeEvent.addListener(listener);
        return listener;
    }
    
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        channel = null;
        closeEvent.post(addr);
    }

    public boolean isClosed() {
        return channel==null;
    }

    //====================== CompletionHandler's backend
    
	/** new client connected */
	@Override
	public void completed(AsynchronousSocketChannel result, Callback<AsyncSocketChannel> acceptor) {
	    synchronized(this) {
	        maxConn--;
	        if (maxConn>0) { 
	            channel.accept(acceptor, this);
	        }        
	    }
	    acceptor.post(new AsyncSocketChannel2(result));
	}

	/** new client connection failed */
	@Override
	public void failed(Throwable exc, Callback<AsyncSocketChannel> acceptor) {
	    acceptor.postFailure(exc);
		if (exc instanceof AsynchronousCloseException) {
			// channel closed.
			close();
		} else {
		    // continue
	        synchronized(this) {
	            maxConn--;
	            if (maxConn>0) { 
	                channel.accept(acceptor, this);
	            }        
	        }
		}
	}
}
