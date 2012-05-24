/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.swing;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.awt.EventQueue;
import com.github.rfqu.df4j.core.*;

public class SwingSupport {
    /** sets default executor for tasks created on the swing EDT.
     * @param executor
     */
    public static void setEDTDefaultExecutor(final ExecutorService executor) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Task.setCurrentExecutorService(executor);
            }
            
        });
    }

    private static volatile SwingExecutor swingExecutor;
    
    public static Executor getSwingExecutor() {
    	SwingExecutor res=swingExecutor;
    	if (res==null) {
        	synchronized (SwingSupport.class) {
        		res=swingExecutor;
        	    if (res==null) {
        	    	res=swingExecutor=new SwingExecutor();
        	    }
    		}
    	}
		return res;
    }
    /**
     * Processes task on the Event Dispatch Thread.
     */
	static class SwingExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
	        EventQueue.invokeLater(command);
		}
	}
}
