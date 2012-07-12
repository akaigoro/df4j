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
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

/**
 * Wrapper over AsynchronousSocketChannel.
 * Simplifies input-output, handling queues of requests.
 * @author rfq
 *
 */
public class AsyncSocketChannel extends Link 
   implements CompletionHandler<Void, AsynchronousSocketChannel>
{
    protected AsynchronousSocketChannel channel;
    protected volatile boolean connected=false;
    protected volatile boolean closed=false;
    protected Throwable connectionFailure=null;
    protected final Reader reader=new Reader();
    protected final Writer writer=new Writer();
    protected Promise<AsynchronousSocketChannel> connectListeners;

    public AsyncSocketChannel() {
    }

    /**
     * for server-side socket
     * @param assch
     */
    public AsyncSocketChannel(AsynchronousSocketChannel assch) {
        completed(null, assch);
    }

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousSocketChannel.open(acg);
        channel.connect(addr, channel, this);
    }
    
    /**
     * callback method for successful connection in client-side mode
     */
    @Override
    public void completed(Void result, AsynchronousSocketChannel attachement) {
        Promise<AsynchronousSocketChannel> listeners;
        synchronized(this) {
            channel=attachement;
            connected=true;
            listeners=connectListeners;
        }
        reader.resume();
        writer.resume();
        if (listeners!=null) {
            listeners.send(attachement);
            connectListeners=null; // not needed anymore
        }           
    }
    
    public void addConnectListener(Port<AsynchronousSocketChannel> listener) {
        checkConnected:
    	synchronized (this) {
    	    if (connected) {
    	        break checkConnected;
    	    }
            if (connectListeners == null) {
                connectListeners = new Promise<AsynchronousSocketChannel>();
            }
            connectListeners.addListener(listener);
            return;
        }
        listener.send(channel);
    }
    
    /**
     * callback method for failed connection
     */
    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        connectionFailure=exc;
        //exc.printStackTrace();
    } 

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void close() throws IOException {
        closed=true;
        if (channel!=null) {
            channel.close();
        }
    }

    protected void checkState() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public <T extends SocketIORequest<T>> T write(T socketIORequest, Port<T> replyTo) throws ClosedChannelException {
        checkState();
        socketIORequest.prepare(this, false, replyTo);
        writer.send(socketIORequest);
        return socketIORequest;
    }
    
    public <T extends SocketIORequest<T>> T write(T request, Port<T> replyTo, long timeout) throws ClosedChannelException {
        checkState();
        request.prepare(this, false, replyTo, timeout);
        writer.send(request);
        return request;
    }
    
    public <T extends SocketIORequest<T>> T read(T request, Port<T> replyTo) throws ClosedChannelException {
        checkState();
        request.prepare(this, true, (Port<T>) replyTo);
        reader.send(request);
        return request;
    }
    
    public <T extends SocketIORequest<T>> T read(T request, Port<T> replyTo, long timeout) throws ClosedChannelException {
        checkState();
        request.prepare(this, true, replyTo, timeout);
        reader.send(request);
        return request;
    }
    
    public boolean isConnected() {
        return connected;
    }

    public boolean isClosed() {
        return closed;
    }

    abstract class RequestQueue extends Actor<SocketIORequest<?>>
       implements CompletionHandler<Integer, SocketIORequest<?>>
    {
        protected Switch channelAcc=new Switch(); // channel accessible
        protected SocketIORequest<?> currentRequest;
        
        protected void resume() {
            channelAcc.on();
        }
        
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
        	if (AsyncSocketChannel.this.closed) {
        		request.failed(new AsynchronousCloseException(), AsyncSocketChannel.this);
        		return;
        	}
        	currentRequest=request;
            channelAcc.off(); // block channel
            act2(request);
        }
        
        protected abstract void act2(SocketIORequest<?> request) throws Exception;

		@Override
        public void completed(Integer result, SocketIORequest<?> request) {
		    currentRequest=null;
            channelAcc.on();
            request.completed(result, AsyncSocketChannel.this);
        }

        @Override
        public void failed(Throwable exc, SocketIORequest<?> request) {
            channelAcc.on();
            if (exc instanceof AsynchronousCloseException) {
                try {
                    AsyncSocketChannel.this.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            request.failed(exc, AsyncSocketChannel.this);
        }
    }
    
    public final class Reader extends RequestQueue {
        @Override
        protected void act2(SocketIORequest<?> request) throws Exception {
//          System.out.println("channel read started id="+request.id);
            if (request.timed) {
                channel.read(request.buffer, request.timeout, TimeUnit.MILLISECONDS, request, this);
            } else {
                channel.read(request.buffer, request, this);
            }
        }
        
    }
    
    public final class Writer extends RequestQueue {
        @Override
        protected void act2(SocketIORequest<?> request) throws Exception {
//          System.out.println("channel read started id="+request.id);
            if (request.timed) {
                channel.write(request.buffer, request.timeout, TimeUnit.MILLISECONDS, request, this);
            } else {
                channel.write(request.buffer, request, this);
            }
        }
        
    }
    
}
