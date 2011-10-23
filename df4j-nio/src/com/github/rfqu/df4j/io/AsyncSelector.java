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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.core.Task;

public class AsyncSelector extends Thread {
    private ExecutorService executor;
    protected Selector selector;
    private MessageQueue<AsyncChannel> rrs=new MessageQueue<AsyncChannel>();

    public AsyncSelector(ExecutorService executor) throws IOException {
        setName(getName()+" AsyncSelector");
        setDaemon(true);
        this.executor=executor;
        this.selector = Selector.open();
    }

    AsyncSelector() throws IOException {
        this(Task.getCurrentExecutor());
    }

    void notify(AsyncChannel asch) throws ClosedChannelException {
        synchronized (rrs) {
            rrs.enqueue(asch);
        }
        selector.wakeup();
    }

    @Override
    public void run() {
        Task.setCurrentExecutor(executor);
        // processing
        try {
            while (true) {
                // wait for events
                int nk = selector.select();
                if (nk>0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();
                        if (!key.isValid()) continue;
                        AsyncChannel channel = (AsyncChannel) key.attachment();
                        try {
                            channel.notify(key);
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        }
                    }
                }
                for (;;) {
                    AsyncChannel asch;
                    synchronized (rrs) {
                        asch=rrs.poll();
                    }
                    if (asch==null) {
                        break;
                    }
                    try {
                        asch.doRegistration(selector);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                }
            }
        } catch (ClosedSelectorException e) {
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }
    

    public void close() throws IOException {
        selector.close();
    }

}
