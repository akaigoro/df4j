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

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.connector.reactivestream.ReactiveInput;
import org.df4j.core.connector.reactivestream.ReactiveSubscription;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Accepts incoming connections, pushes them pu subscribers
 *
 * though it extends AsyncTask, it is effectively an Actor
 */
public class AsyncServerSocketChannel
        extends AsyncTask<AsynchronousSocketChannel>
        implements ScalarPublisher<AsynchronousSocketChannel>,
        CompletionHandler<AsynchronousSocketChannel, AsyncSocketChannel>
{
    protected final Logger LOG = Logger.getLogger(AsyncServerSocketChannel.class.getName());

    /** place for demands */
    private StreamInput<ScalarSubscriber<? super AsynchronousSocketChannel>> requests = new StreamInput<>(this);
    
    protected volatile AsynchronousServerSocketChannel assc;
    /** max number of connections in input queue */
    ReactiveSubscription subscription;

    public AsyncServerSocketChannel(SocketAddress addr) throws IOException {
        if (addr == null) {
            throw new NullPointerException();
        }
        assc = AsynchronousServerSocketChannel.open();
        assc.bind(addr);
        this.start();
        LOG.config("AsyncServerSocketChannel("+addr+") created");
    }


    @Override
    public <S extends ScalarSubscriber<? super AsynchronousSocketChannel>> S subscribe(S subscriber) {
        requests.post(subscriber);
        return subscriber;
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

    //====================== Dataflow backend

    @Action
    public void accept(AsyncSocketChannel connection) {
        try {
            assc.accept(connection, this);
        } catch (Exception e) {
            close();
        }
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, AsyncSocketChannel connection) {
        LOG.info("AsynchronousServerSocketChannel: accepted "+connection.toString());
        connection.post(result);
        this.start(); // allow  next assc.accpt()
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, AsyncSocketChannel connection) {
        connection.postFailure(exc);
        if (exc instanceof AsynchronousCloseException) {
            // channel closed.
            close();
        } else {
            this.start(); // TODO should we allow  next assc.accpt()?
        }
    }
}
