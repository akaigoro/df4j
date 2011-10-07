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
    Selector selector;
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

    void startRegistration(AsyncChannel asch) throws ClosedChannelException {
        synchronized (rrs) {
            rrs.enqueue(asch);
        }
        selector.wakeup();
    }

    @Override
    public void run() {
        Task.setCurrentExecutor(executor);
        currentSelectorKey.set(this);
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

    public static AsyncSelector getCurrentSelector() {
        return currentSelectorKey.get();
    }

    public static void setCurrentSelector(AsyncSelector s) {
        currentSelectorKey.set(s);
    }

    private static final ThreadLocal <AsyncSelector> 
        currentSelectorKey  = new ThreadLocal <AsyncSelector> ();
}
