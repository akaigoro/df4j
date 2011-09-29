package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.core.Task;

public class AsyncSelector extends Thread {
    private ExecutorService executor;
    Selector selector;
    private MessageQueue<AsyncChannel> rrs=new MessageQueue<AsyncChannel>();

    AsyncSelector() throws IOException {
        this.executor=Task.getCurrentExecutor();
        this.selector = Selector.open();
        super.setDaemon(true);
        super.start();
    }

    void startRegistration(AsyncChannel asch) throws ClosedChannelException {
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
                        AsyncChannel channel = (AsyncChannel) key.attachment();
                        channel.notify(key);
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
                    asch.endRegistration(selector);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }
    
    static class RegRequest extends Link {
        AsyncChannel asch;
        int interest;
        
        public RegRequest(AsyncChannel asch, int interest) {
            this.asch = asch;
            this.interest = interest;
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
