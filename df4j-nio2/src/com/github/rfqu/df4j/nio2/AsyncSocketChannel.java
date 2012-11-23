/*
 * Copyright 2011-2012 by Alexei Kaigorodov
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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.ext.ActorVariableLQ;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

/**
 * Wrapper over {@link java.nio.channels.AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 */
public class AsyncSocketChannel extends Link 
   implements Port<SocketIORequest<?>>,
//     EventSource<AsynchronousSocketChannel, Callback<AsynchronousSocketChannel>>,
	 CompletionHandler<Void, AsynchronousSocketChannel>
{
    protected AsynchronousSocketChannel channel;
    /** for client-side socket: signals connection completion */
    private Promise<AsynchronousSocketChannel> connEvent=new Promise<AsynchronousSocketChannel>();
    /** read requests queue */
    protected final RequestQueue reader=new RequestQueue();
    /** write requests queue */
    protected final RequestQueue writer=new RequestQueue();
    protected volatile boolean closed=false;

    /**
     * for server-side socket
     * @param assch
     */
    public AsyncSocketChannel(AsynchronousSocketChannel assch) {
        init(assch);
    }
    
    void init(AsynchronousSocketChannel attachement) {
        synchronized(this) {
            channel=attachement;
        }
        reader.resume();
        writer.resume();
        connEvent.send(attachement);
    }
    
    /**
     * for client-side socket
     * Starts connection to a server.
     * IO requests can be queued immediately,
     * but will be executed only after connection completes.
     * If interested in the moment when connection is established,
     * add a listener. 
     * @throws IOException
     */
    public AsyncSocketChannel(SocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=DFContext.getCurrentACGroup();
        channel=AsynchronousSocketChannel.open(acg);
        channel.connect(addr, channel, this);
    }

    // ================== I/O
    
    @Override
	public void send(SocketIORequest<?> request) {
		if (closed) {
			request.failed(new ClosedChannelException());
			return;
		}
		(request.isReadOp()?reader:writer).send(request);
	}    

    public void close() {
        closed=true;
        if (channel!=null) {
            try {
                channel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public synchronized void addListener(Callback<AsynchronousSocketChannel> listener) {
        connEvent.addListener(listener);
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel!=null;
    }

    public boolean isClosed() {
        return closed;
    }

	//========================= backend
	
	/**
     * callback method for successful connection in client-side mode
     */
    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        init(channel);           
    }

    /**
     * callback method for failed connection in client-side mode
     */
    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        connEvent.sendFailure(exc);
    } 

    //===================== inner classes
    
    class RequestQueue extends ActorVariableLQ<SocketIORequest<?>>
       implements CompletionHandler<Integer, SocketIORequest<?>>
    {
        protected Semafor channelAcc=new Semafor(); // channel accessible
        protected SocketIORequest<?> currentRequest;
        
        protected void resume() {
            channelAcc.up();
        }
        
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
        	if (AsyncSocketChannel.this.closed) {
        		request.failed(new AsynchronousCloseException());
        		return;
        	}
        	currentRequest=request;
            if (request.isReadOp()) {
//              System.out.println("channel read started id="+request.id);
                if (request.timed) {
                    channel.read(request.buffer,
                            request.timeout, TimeUnit.MILLISECONDS, request, this);
                } else {
                    channel.read(request.buffer, request, this);
                }
            } else {
//              System.out.println("channel write started id="+request.id);
                if (request.timed) {
                    channel.write(request.buffer, request.timeout, TimeUnit.MILLISECONDS,
                            request, this);
                } else {
                    channel.write(request.buffer, request, this);
                }
            }
        }
        
		@Override
        public void completed(Integer result, SocketIORequest<?> request) {
		    currentRequest=null;
            channelAcc.up();
            request.completed(result);
        }

        @Override
        public void failed(Throwable exc, SocketIORequest<?> request) {
            if (exc instanceof AsynchronousCloseException) {
                AsyncSocketChannel.this.close();
            }
		    currentRequest=null;
            channelAcc.up();
            request.failed(exc);
        }
    }

}
