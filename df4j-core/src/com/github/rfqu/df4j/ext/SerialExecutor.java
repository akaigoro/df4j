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
import java.util.concurrent.RejectedExecutionException;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Task;

/**
 * Serves a set of Actors, running synchronously (one at a time).
 */
public class SerialExecutor extends Actor<Link> implements Executor {

	public SerialExecutor() {
	}

	public SerialExecutor(Executor executor) {
		super(executor);
	}

	/**
	 * Executes the given command at some time in the future.
	 * 
	 * @param command the runnable
	 * @throws NullPointerException if command is null
	 * @throws RejectedExecutionException if this
	 * task cannot be accepted for execution. 
	 */
	@Override
	public void execute(Runnable command) {
		if (command instanceof Task) {
			post((Task) command);
		} else {
			post(new TaskWrapper(command));
		}
	}

	@Override
	protected void act(Link task) throws Exception {
		((Runnable) task).run();
	}

	@Override
	protected void complete() throws Exception {
		super.close();
	}

	static class TaskWrapper extends Link implements Runnable {
		Runnable command;

		public TaskWrapper(Runnable command) {
			this.command = command;
		}

		@Override
		public void run() {
			command.run();
		}
	}

}
