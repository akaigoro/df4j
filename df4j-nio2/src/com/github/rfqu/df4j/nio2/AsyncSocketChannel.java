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
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.core.Actor;
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
    protected boolean connected=false;
    protected boolean closed=false;
    protected Throwable connectionFailure=null;
    protected final Reader reader=new Reader();
    protected final Writer writer=new Writer();

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
     * For server-side socket.
     */
    public void connect(AsyncServerSocketChannel assc) {
        assc.send(this);
    }
    
    /**
     * callback method for successful connection in both modes
     */
    @Override
    public void completed(Void result, AsynchronousSocketChannel attachement) {
        channel=attachement;
        connected=true;
        reader.resume();
        writer.resume();
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

    public <R extends SocketIORequest> R write(R request, Port<? extends SocketIORequest> replyTo) throws ClosedChannelException {
        checkState();
        request.prepare(this, false, (Port<SocketIORequest>) replyTo);
        writer.send(request);
        return request;
    }
    
    public <R extends SocketIORequest> R write(R request, Port<? extends SocketIORequest> replyTo, long timeout) throws ClosedChannelException {
        checkState();
        request.prepare(this, false, (Port<SocketIORequest>) replyTo, timeout);
        writer.send(request);
        return request;
    }
    
    public <R extends SocketIORequest>R read(R request, Port<? extends SocketIORequest> replyTo) throws ClosedChannelException {
        checkState();
        request.prepare(this, true, (Port<SocketIORequest>) replyTo);
        reader.send(request);
        return request;
    }
    
    public <R extends SocketIORequest> R read(R request, Port<? extends SocketIORequest> replyTo, long timeout) throws ClosedChannelException {
        checkState();
        request.prepare(this, true, (Port<SocketIORequest>) replyTo, timeout);
        reader.send(request);
        return request;
    }
    
    public SocketIORequest write(ByteBuffer buf, Port<SocketIORequest> replyTo) throws ClosedChannelException {
        return write(new SocketIORequest(buf), replyTo);
    }
    
    public SocketIORequest write(ByteBuffer buf, Port<SocketIORequest> replyTo, long timeout) throws ClosedChannelException {
        return write(new SocketIORequest(buf), replyTo, timeout);
    }
    
    public <R extends SocketIORequest> SocketIORequest read(ByteBuffer buf, Port<? extends SocketIORequest> replyTo) throws ClosedChannelException {
        return read(new SocketIORequest(buf), replyTo);
    }
    
    public <R extends SocketIORequest> SocketIORequest read(ByteBuffer buf, Port<R> replyTo, long timeout) throws ClosedChannelException {
        return read(new SocketIORequest(buf), replyTo, timeout);
    }
    
    public boolean isConnected() {
        return connected;
    }

    public boolean isClosed() {
        return closed;
    }

    abstract class RequestQueue extends Actor<SocketIORequest>
       implements CompletionHandler<Integer, SocketIORequest>
    {
        protected Switch channelAcc=new Switch(); // channel accessible

        protected void resume() {
            channelAcc.on();
        }
        
        @Override
        protected void act(SocketIORequest request) throws Exception {
        	if (AsyncSocketChannel.this.closed) {
        		request.failed(new AsynchronousCloseException(), AsyncSocketChannel.this);
        		return;
        	}
            channelAcc.off(); // block channel
            act2(request);
        }
        
        protected abstract void act2(SocketIORequest request) throws Exception;

		@Override
        public void completed(Integer result, SocketIORequest request) {
            channelAcc.on();
            request.completed(result, AsyncSocketChannel.this);
        }

        @Override
        public void failed(Throwable exc, SocketIORequest request) {
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
        protected void act2(SocketIORequest request) throws Exception {
        	if (AsyncSocketChannel.this.closed) {
        		request.failed(new AsynchronousCloseException(), AsyncSocketChannel.this);
        		return;
        	}
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
        protected void act2(SocketIORequest request) throws Exception {
//          System.out.println("channel read started id="+request.id);
            if (request.timed) {
                channel.write(request.buffer, request.timeout, TimeUnit.MILLISECONDS, request, this);
            } else {
                channel.write(request.buffer, request, this);
            }
        }
        
    }
    
}
