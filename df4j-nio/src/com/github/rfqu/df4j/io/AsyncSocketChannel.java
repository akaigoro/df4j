/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Actor;

public class AsyncSocketChannel extends AsyncChannel {    
    protected SocketChannel channel;
    protected Throwable connectionFailure=null;
    protected RequestQueue readRequests=new RequestQueue(true);
    protected RequestQueue writeRequests=new RequestQueue(false);

    public AsyncSocketChannel(AsyncSelector selector) {
        super(selector);
    }

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        SocketChannel channel = SocketChannel.open(remote);
        connCompleted(channel);
    }

    /**
     * for server-side socket
     * start connection, to be continued in connCompleted()
     * @throws ClosedChannelException 
     * @throws IOException
     */
    public void connect(AsyncServerSocketChannel s) throws IOException {
        s.accept(this);        
    }

    /**
     * for all types of sockets
     * finishes connection
     * @throws IOException
     */
    protected void connCompleted(SocketChannel channel) throws IOException {
        this.channel=channel;
        channel.configureBlocking(false);
        interestOn(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        readRequests.setReady(true);
        writeRequests.setReady(true);
    }

    /**
     * for server-side socket
     * finishes connection
     */
    protected synchronized void connFailed(Throwable exc) {
        connectionFailure=exc;
        // TODO fail enqueued requests
    }

    public void read(SocketIORequest request) {
        request.start(this, true);
        readRequests.send(request);
    }

    public void write(SocketIORequest request) {
        request.start(this, false);
        writeRequests.send(request);
    }

    @Override
    synchronized void notify(SelectionKey key) {
        if (channel==null) {
            return;
        }
        if (key.isReadable()) {
            readRequests.setReady(true);
        }
        if (key.isWritable()) {
            writeRequests.setReady(true);
        }
    }

    protected void requestCompleted(SocketIORequest request) {
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    public synchronized void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel=null;
        }
    }

    class RequestQueue extends Actor<SocketIORequest> {
        final boolean read;

        public RequestQueue(boolean read) {
            this.read = read;
        }

        @Override
        protected void act(SocketIORequest request) {
            try {
                int res=read? channel.read(request.buffer): channel.write(request.buffer);
                if (res == 0) {
                    synchronized (this) {
                        input.push(request);
                        setReady(false);
                    }
                } else {
                    request.completed(res);
                }
            } catch (Exception e) {
                request.failed(e);
            }
        }
    }

}
