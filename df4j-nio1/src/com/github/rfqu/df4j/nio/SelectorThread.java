package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;
import com.github.rfqu.df4j.core.DoublyLinkedQueue;
import com.github.rfqu.df4j.core.Task;

public class SelectorThread implements Runnable, Executor {
    DFContext context;
    private Thread thrd;
	// The selector we'll be monitoring
	private Selector selector;
    private boolean running=false;
	private DoublyLinkedQueue<Task> tasks=new DoublyLinkedQueue<Task>();

    public SelectorThread(DFContext context) throws IOException {
        this.context=context;
        // Create a new selector
        this.selector = Selector.open();
		thrd=new Thread(this);
		thrd.setDaemon(true);
		thrd.setName("SelectorThread");
		thrd.start(); // TODO kill suicide when not used
    }

    @Override
    public void execute(final Runnable command) {
        Task task = (command instanceof Task) ? ((Task) command):
          new Task() {
            @Override
            public void run() {
                command.run();
            }
        };
        boolean doFire;
        synchronized (this) {
            tasks.add(task);
            doFire = !running;
            running = true;
        }
        if (doFire) {
            selector.wakeup();
        }
    }

    synchronized Task nextTask() {
        Task task=tasks.poll();
        if (task==null) {
            running=false;
        }
        return task;
    }
    
    void registerNow(SelectableChannel socket, int ops, SelectorEventListener att) throws ClosedChannelException {
        SelectionKey key = socket.keyFor(selector);
        if (key==null || !key.isValid()) {
            socket.register(selector, ops, att);
        } else {
            int interestOps = key.interestOps();
            key.interestOps(ops|interestOps);
        }
    }

    void interestOff(SelectableChannel socket, int noInterestOps) {
        SelectionKey key = socket.keyFor(selector);
        if (key==null) {
            return;
        }
        int interestOps = key.interestOps();
        key.interestOps(interestOps & ~noInterestOps);
    }
    
	public void run() {
	    DFContext.setCurrentContext(context);
		while (selector.isOpen() && !Thread.interrupted()) {
            for (;;) {
                Runnable task=nextTask();
                if (task==null) {
                    break;
                }
                task.run();
            }

            try {
                if (selector.select()==0) {
                    continue;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Iterate over the set of keys for which events are available
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();

                if (!key.isValid()) {
                    continue;
                }

                // Pass event to the listener
                ((SelectorEventListener)key.attachment()).onSelectorEvent(key);
            }
		}
	}
	
    //--------------------- context
    
    private static ItemKey<SelectorThread> selectorThreadKey
        = DFContext.getCurrentContext().new ItemKey<SelectorThread>()
    {
        @Override
        protected SelectorThread initialValue(DFContext context) {
            try {
                return new SelectorThread(context);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        
    };
    
    public static SelectorThread getCurrentSelectorThread() {
        return selectorThreadKey.get();
    }
}
