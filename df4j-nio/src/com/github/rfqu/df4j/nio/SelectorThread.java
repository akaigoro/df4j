package com.github.rfqu.df4j.nio;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;

public class SelectorThread implements Runnable, Executor {
    private Thread thrd;
	// The selector we'll be monitoring
	private Selector selector;
    private Lock lock = new ReentrantLock();
    private boolean fired=false;
	private LinkedList<Runnable> tasks=new LinkedList<Runnable>();

    public SelectorThread(DFContext context) throws IOException {
        // Create a new selector
        this.selector = Selector.open();
		thrd=new Thread(this);
		thrd.setDaemon(true);
		thrd.setName("SelectorThread");
		thrd.start(); // TODO kill suicide when not used
    }


    @Override
    public void execute(Runnable command) {
        boolean doFire;
        lock.lock();
        try {
            tasks.add(command);
            if (fired) {
                doFire=false;
            } else {
                doFire=fired=true;
            }
        } finally {
          lock.unlock();
        }
        if (doFire) {
            selector.wakeup();
        }
    }

    void registerNow(SelectableChannel socket, int ops, Object att) throws ClosedChannelException {
        SelectionKey key = socket.keyFor(selector);
        if (key==null) {
            socket.register(selector, ops, att);
        } else {
            key.interestOps(ops|key.interestOps());
        }
    }

    void register(final SelectableChannel socket, final int ops, final Object att) {
        final ClosedChannelException thr=new ClosedChannelException();
        execute(new Runnable() {
            @Override
            public void run() {
                try {
                    registerNow(socket, ops, att);
                } catch (ClosedChannelException e) {
                    System.err.println("register:");
                    thr.printStackTrace();
                }
            }
        });
    }
    
    void interestOff(SelectableChannel socket, int noInterestOps) {
        SelectionKey key = socket.keyFor(selector);
        if (key==null) {
            return;
        }
        int interestOps = key.interestOps();
        if (interestOps==noInterestOps) {
            key.cancel();
        } else {
            key.interestOps(interestOps & ~noInterestOps);
        }
    }

    
	public void run() {
		while (selector.isOpen() && !Thread.interrupted()) {
			try {
                // dances with tambourine
			    synchronized(selector) {
			    }
			    
			    for (;;) {
                    Runnable task;
			        lock.lock();
			        try {
			            task=tasks.poll();
			            if (task==null) {
			                fired=false;
			                break;
			            }
			        } finally {
			          lock.unlock();
			        }
			        task.run();
			    }
				selector.select();
                synchronized(selector) {
                }

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					Object listener=key.attachment();
					if (listener==null) {
				        System.err.println("SelectorThread: listener=null for "+key);
				        continue;
					}
					// Pass event to the listener
                    SelectorEventListener sselistener=(SelectorEventListener) listener;
                    if (!key.isValid()) {
                        System.err.println("key spoiled");
                    }
                    try {
                        sselistener.onSelectorEvent(key);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
    //--------------------- context
    
    private static ItemKey<SelectorThread> selectorKey
        =DFContext.getCurrentContext().new ItemKey<SelectorThread>()
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
        return selectorKey.get();
    }
}
