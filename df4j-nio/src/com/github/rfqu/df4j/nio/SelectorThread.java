package com.github.rfqu.df4j.nio;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;

public class SelectorThread implements Runnable {
    private Thread thrd;
	// The selector we'll be monitoring
	private Selector selector;
	private LinkedBlockingQueue<Runnable> tasks=new LinkedBlockingQueue<Runnable>();

    public SelectorThread(DFContext context) throws IOException {
        // Create a new selector
        this.selector = Selector.open();
		thrd=new Thread(this);
		thrd.setDaemon(true);
		thrd.setName("SelectorThread");
		thrd.start(); // TODO kill suicide when not used
    }

    void register(final SelectableChannel socket, final int ops, final SocketEventListener att){
        final Throwable thr=new Throwable();
        tasks.add(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.register(selector, ops, att);
                } catch (ClosedChannelException e) {
                    System.err.println("ClosedChannelException:");
                    thr.printStackTrace();
                }
            }
        });
        selector.wakeup();
    }


    public void register(final SelectableChannel socket, final int ops) {
        final Throwable thr=new Throwable();
        tasks.add(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.register(selector, ops);
                } catch (ClosedChannelException e) {
                    System.err.println("ClosedChannelException:");
                    thr.printStackTrace();
                }
            }
        });
        selector.wakeup();
    }
    public void deregister(final SelectableChannel socket) {
        tasks.add(new Runnable() {
            @Override
            public void run() {
                SelectionKey key = socket.keyFor(selector);
                if (key!=null) {
                    key.cancel();
                }
            }
        });
        selector.wakeup();
    }

	public void run() {
		while (true) {
			try {
                // dances with tambourine
			    synchronized(selector) {
			    }
			    
			    for (Runnable task: tasks) {
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

					SocketEventListener listener=(SocketEventListener) key.attachment();
					if (listener==null) {
				        System.err.println("listener=null for "+key);
				        Thread.sleep(100);
				        continue;
					}
					// Check what event is available and deal with it
					if (key.isAcceptable()) {
					    listener.accept(key);
					} else {
					    if (key.isConnectable()) {
	                        listener.connect(key);
					    }
					    if (key.isReadable()) {
	                        listener.read(key);
					    }
					    if (key.isWritable()) {
					        listener.write(key);
					    }
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
