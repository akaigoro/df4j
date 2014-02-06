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

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.DataflowVariable;

/**
 * abstract node with multiple inputs, single output and exception handling
 * Unlike Actor, it is single shot. 
 * @param <R> type of result
 */
abstract class Function<T> extends CompletableFuture<T> {
	MyDataflowNode node;

	public Function() {
		node=new MyDataflowNode(null);
	}

	public Function(Executor executor) {
		node=new MyDataflowNode(executor);
	}

	public Function(Object... args) {
		node=new MyDataflowNode(null);
		node.setArgs(args);
	}

	public Function(Executor executor, Object... args) {
		node=new MyDataflowNode(executor);
		node.setArgs(args);
	}

	public Function<T> setArgs(Object... args) {
		node.setArgs(args);
		return this;
	}

	abstract protected T eval(Object[] args);
	
	class MyDataflowNode extends DataflowVariable {
		Object[] args;

	    public MyDataflowNode(Executor executor) {
	        super(new SingleTask(executor));
	    }

	    public MyDataflowNode() {
	        super(new SingleTask(null));
	    }

		public void setArgs(Object[] args) {
			if (this.args!=null) {
				throw new IllegalArgumentException("arguments are set already");
			}
			this.args = args;
			lockFire();
			for (int k=0; k<args.length; k++) {
				Object arg=args[k];
				if (arg instanceof CompletableFuture) {
					@SuppressWarnings("unchecked")
					CompletableFuture<Object> argf=(CompletableFuture<Object>) arg;
					if (argf.isDone()) {
						try {
							args[k]=argf.get();
						} catch (Exception e) {
							postFailure(e);
						}
					} else {
						args[k]=null;
						CallbackInput input=new CallbackInput(k);
						argf.addListener(input);
					}
				}
			}
			unlockFire();
		}

		@Override
		protected void act() {
			Function.this.post(eval(args));
		}
		
	    @Override
		protected void handleException(Throwable exc) {
	    	Function.this.postFailure(exc);
		}

		/** Scalar Input which also redirects failures 
	     */
	    public class CallbackInput extends Semafor implements Callback<Object> {
	    	int idx;
	    	
	        public CallbackInput(int idx) {
				this.idx = idx;
			}

			@Override
			public void post(Object token) {
				args[idx]=(token);
				super.up();
			}

			@Override
	        public void postFailure(Throwable exc) {
	        	MyDataflowNode.this.postFailure(exc);
	        }
	    }
	}
}