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

import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.connector.reactivestream.ReactiveInput;
import org.df4j.core.connector.reactivestream.Subscriber;
import org.df4j.core.connector.reactivestream.Subscription;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Accepts incoming connections
 */
public class AsyncServerSocketChannel extends AsyncTask
        implements Subscriber<ServerConnection>
        ,CompletionHandler<AsynchronousSocketChannel, ServerConnection>
{
    protected final Logger LOG = Logger.getLogger(AsyncServerSocketChannel.class.getName());

    protected final ReactiveInput<ServerConnection> mainInput = new ReactiveInput<ServerConnection>(this);
    /**
     * prevents simultaneous channel.accept()
     */
    protected Semafor acceptAllowed = new Semafor(this);

    /**
     * limits the total number of active connections.
     * protected Semafor allowedCnnections = new Semafor(this);
     */
    protected volatile AsynchronousServerSocketChannel assc;
    /** max number of connections in input queue */
    int connCount;

    public AsyncServerSocketChannel(SocketAddress addr, int connCount) throws IOException {
        if (addr == null) {
            throw new NullPointerException();
        }
        this.connCount = connCount;
        assc = AsynchronousServerSocketChannel.open();
        assc.bind(addr);
        acceptAllowed.release();
        LOG.config("AsyncServerSocketChannel("+connCount+") created");
    }

    public synchronized void close() {
        if (assc == null) {
            return;
        }
        AsynchronousServerSocketChannel asscLock = assc;
        assc = null;
        try {
            asscLock.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        mainInput.onSubscribe(subscription);
        subscription.request(connCount);
    }

    @Override
    public void post(ServerConnection m) {
        mainInput.post(m);
    }

    @Override
    public void postFailure(Throwable ex) {
        mainInput.postFailure(ex);
    }

    /**
     * processes closing signal
     * @throws Exception
     */
    @Override
    public void complete() {
        mainInput.complete();
    }

    public boolean isClosed() {
        return mainInput.isClosed();
    }

    /**
     * the input stream completed
     */
    protected void onCompleted() {}

    //====================== Dataflow backend

    /**
     * process the retrieved tokens.
     * @throws Exception
     */
    @Action
    protected void act(ServerConnection message) {
        if (message==null) {
            onCompleted();
        } else {
            try {
                assc.accept(message, this);
            } catch (Exception e) {
                close();
            }
        }
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, ServerConnection conn) {
        LOG.info("AsynchronousServerSocketChannel: accepted "+conn);
        conn.init(result);
        acceptAllowed.release(); // allow assc.accpt()
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, ServerConnection conn) {
        conn.postFailure(exc);
        if (exc instanceof AsynchronousCloseException) {
            // channel closed.
            close();
        } else {
            acceptAllowed.release();
        }
    }
}
