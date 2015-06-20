/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.func;

import java.util.concurrent.Executor;
import org.df4j.core.Listener;
import org.df4j.core.actor.Actor;

/**
 * abstract node with multiple inputs, single output and exception handling
 * Unlike Actor, it is single shot. 
 * @param <R> type of result
 */
public abstract class Function<T> extends Promise<T> {
	MyDataflowNode node;

	public Function() {
		node=new MyDataflowNode(null);
	}

	public Function(Executor executor) {
		node=new MyDataflowNode(executor);
	}

	public Function(Object... args) throws Exception {
		node=new MyDataflowNode(null);
		node.setArgs(args);
	}

	public Function(Executor executor, Object... args) throws Exception {
		node=new MyDataflowNode(executor);
		node.setArgs(args);
	}

	public Function<T> setArgs(Object... args) {
		node.setArgs(args);
		return this;
	}

	abstract protected Object eval(Object[] args) throws Exception;
	
	class MyDataflowNode extends Actor {
	    Semafor doEval=new Semafor(); // in order not to loop in loopAct
		Object[] args;

	    public MyDataflowNode(Executor executor) {
	        super(executor);
	    }

	    public MyDataflowNode() {
	    }

	    /**
	     * @param args arguments of the function
	     *  if argument is of type ListenableFuture (typically another Function),
	     *  execution is suspended until value is available.
	     */
		public void setArgs(Object[] args) {
			if (this.args!=null) {
				throw new IllegalArgumentException("arguments are set already");
			}
			this.args = args;
			boolean completed=false;
			lockFire();
			for (int k=0; k<args.length; k++) {
				Object arg=args[k];
				if (arg instanceof ListenableFuture) {
                    args[k]=null;
					@SuppressWarnings("unchecked")
					ListenableFuture<Object> argf=(ListenableFuture<Object>) arg;
					if (argf.isDone()) {
                        try {
                            args[k]=argf.get();
                        } catch (Exception e) {
                            completed=true;
                            Function.this.postFailure(e);
                        }
					} else {
						CallbackInput input=new CallbackInput(k);
						argf.addListener(input);
					}
				}
			}
			if (!completed) {
	            doEval.up();
			}
			unlockFire();
		}

        @Override
		@SuppressWarnings("unchecked")
		protected void act() {
			Object res;
            try {
                res = eval(args);
                if (res instanceof ListenableFuture) {
                    ((ListenableFuture<T>)res).addListener(Function.this);
                } else {
                    Function.this.post((T)res);
                }
            } catch (Exception e) {
                Function.this.postFailure(e);
            }
		}
		
		/** a listener to arguments passed as ListenableFuture
	     */
	    public class CallbackInput extends Semafor implements Listener<Object> {
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
	            Function.this.postFailure(exc);
	        }
	    }
	}
}