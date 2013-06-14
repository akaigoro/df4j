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

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * Executes tasks synchronously on the caller's thread.
 * Useful for debugging, as execution sequence is always the same.
 */
public class ImmediateExecutor implements Executor {
	protected boolean running=false;
	protected ArrayDeque<Runnable>queue = new ArrayDeque<Runnable>();

    @Override
    public void execute(Runnable command) {
    	synchronized (queue) {
			if (running) {
				queue.add(command);
				return;
			}
			running=true;
		}
        for (;;) {
            command.run();
        	synchronized (queue) {
        		command=queue.poll();
    			if (command==null) {
        			running=false;
    				return;
    			}
    		}
        }
    }
}
