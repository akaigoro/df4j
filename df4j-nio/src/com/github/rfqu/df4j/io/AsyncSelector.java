package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Task;

public class AsyncSelector extends Thread {
    private ExecutorService executor;
    Selector selector;
    private Object gate=new Object();

    AsyncSelector() throws IOException {
        this.executor=Task.getCurrentExecutor();
        this.selector = Selector.open();
        super.setDaemon(true);
        super.start();
    }

    void register(AsyncChannel asch, int interest) throws ClosedChannelException {
        synchronized (gate) {
            selector.wakeup();
            asch.getChannel().register(selector, interest, asch);
        }
    }

    @Override
    public void run() {
        Task.setCurrentExecutor(executor);
        // processing
        try {
            while (true) {
                // wait for events
                selector.select();
                synchronized (gate) {}
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    AsyncChannel channel = (AsyncChannel) key.attachment();
                    channel.notify(key);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }

    public static AsyncSelector getCurrentSelector() {
        return currentSelectorKey.get();
    }

    private static final ThreadLocal <AsyncSelector> 
        currentSelectorKey  = new ThreadLocal <AsyncSelector> ()
    {
        @Override
        protected AsyncSelector initialValue() {
            try {
                return new AsyncSelector();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
    };
}
