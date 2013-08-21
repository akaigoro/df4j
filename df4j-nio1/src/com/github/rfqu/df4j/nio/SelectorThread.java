/*
 * Copyright 2011-2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;

public class SelectorThread implements Runnable, Executor {
    DFContext context;
    private Thread thrd;
	// The selector we'll be monitoring
	Selector selector;
    private LinkedList<Runnable> tasks=new LinkedList<Runnable>();
    private boolean running=false;

    public SelectorThread(DFContext context) throws IOException {
        this.context=context;
        // Create a new selector
        this.selector = Selector.open();
		thrd=new Thread(this);
		thrd.setDaemon(true);
		thrd.setName("SelectorThread");
		thrd.start(); // TODO kill suicide when not used
    }

    public void execute(final Runnable task) {
        boolean doFire;
        synchronized (this) {
            tasks.add(task);
            doFire = !running;
            running = true;
        }
//        System.err.println("task enqueued:"+task);
        if (doFire) {
            selector.wakeup();
        }
    }

    public void run() {
	    DFContext.setCurrentContext(context);
		while (selector.isOpen() && !Thread.interrupted()) {
            try {
                int selectCount = selector.select();
//System.out.println("SelectorThread: selectCount="+selectCount);
                if (selectCount!=0) {
                    // Iterate over the set of keys for which events are available
                    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey key = selectedKeys.next();
                        selectedKeys.remove();
                        if (!key.isValid()) {
                            break;
                        }

                        AbstractSelectorListener listener=(AbstractSelectorListener)key.attachment();
                        listener.run(key);
                    }
                }
                for (;;) {
                    Runnable task;
                    synchronized (this) {
                        task=tasks.poll();
                        if (task==null) {
                            running=false;
                            break;
                        }
                    }
//                    System.err.println("task started:"+task);
                    task.run();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
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
                e.printStackTrace();
                return null;
            }
        }
        
    };
    
    public static SelectorThread getCurrentSelectorThread() {
        return selectorThreadKey.get();
    }
}
