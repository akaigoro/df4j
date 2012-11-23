/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import java.util.concurrent.Executor;
import java.awt.EventQueue;

import com.github.rfqu.df4j.core.*;

public class SwingSupport {
    /** sets default executor for tasks created on the swing EDT.
     * If not invoked, new DF context will be created for EDT, with default Executor.
     * @param executor
     */
    public static void setEDTDefaultContext(final DFContext context) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
            	DFContext.setCurrentContext(context);
            }
            
        });
    }
    
    public static Executor getSwingExecutor() {
		return new SwingExecutor();
    }
    
    /**
     * Processes task on the Event Dispatch Thread.
     */
	public static class SwingExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
	        EventQueue.invokeLater(command);
		}
	}

	/**
     * Processes messages on EDT.
     */
	public static abstract class EDTActor<T> extends Actor<T> {
    	EDTActor(){
    		super(SwingSupport.getSwingExecutor());
    	}
    }

}
