/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

/**
 * abstract node with several inputs and outputs
 */
public abstract class BaseActor extends Task {
	static final int allOnes=0xFFFFFFFF;
    private int pinCount=0;
    private int pinMask=0;
    private int readyPins=0;
    /** This pin is to prevent premature firing when the first declared pin is initialized as ready */
    private final BooleanPlace initialized=new BooleanPlace();
    protected boolean fired;
    
    public BaseActor(Executor executor) {
    	super(executor);
    }

    public BaseActor() {
    }

    protected boolean isReady() {
		return readyPins==pinMask;
	}

    /**
     * indicates end of pin declarations.
     */
    public void start() {
    	initialized.send();
    }
    
    public void stop() {
    	initialized.remove();
    }
    
    /** loops while all pins are ready
     */
    @Override
    public void run() {
        for (;;) {
            synchronized (this) {
                if (!isReady()) {
                    fired=false; // allow firing
                    return;
                }
                removeTokens();
            }
            act();
        }
    }
    
    /** Should remove at least 1 token.
     * Should return quickly, as is called from synchronized block.
     */
    protected abstract void removeTokens();

    /** process the retrieved tokens.
     * 
     */
    protected abstract void act();

    protected abstract class Pin {
    	
    	private int portBit;
        {
        	int count;
        	synchronized (BaseActor.this) {
            	count = pinCount;
                if (count==32) {
              	  throw new IllegalStateException("only 32 pins culd be created");
                }
                pinCount++;
                portBit = 1<<count;
            	pinMask=pinMask|portBit;
			}
        }

        protected void turnOn() {
        	synchronized (BaseActor.this) {
                readyPins = readyPins | portBit;
                if (!isReady() || fired) {
                    return;
                }
                fired = true; // to prevent multiple concurrent firings
            }
            fire();
        }

        protected void turnOff() {
            synchronized (BaseActor.this) {
                readyPins=readyPins&~portBit;
            }
        }

    }

    /**
     *  Only stops/allows actor execution
     */
    protected class BooleanPlace extends Pin {
    	private boolean on=false;
    	
        public BooleanPlace(boolean on) {
			this.on = on;
        	if (on) {
            	turnOn();
        	}
		}

        public BooleanPlace() {
		}

		public void send() {
            synchronized (BaseActor.this) {
            	if (on) {
    				throw new IllegalStateException("place is occupied already"); 
            	}
            	on=true;
            	turnOn();
            }
        }

        public void remove() {
            synchronized (BaseActor.this) {
            	if (!on) {
    				throw new IllegalStateException("place is not occupied"); 
            	}
            	on=false;
            	turnOff();
            }
        }

		protected boolean isEmpty() {
			return !on;
		}
    }

    /** 
     * holds tokens without data 
     */
    protected class PetriPlace extends Pin {
    	private int count=0;
    	
        public PetriPlace(int count) {
			this.count = count;
        	if (count>0) {
        		turnOn();
        	}
		}

        public PetriPlace() {
		}

		public void send() {
            synchronized (BaseActor.this) {
            	count++;
            	if (count==1) {
            		turnOn();
            	}
            }
        }

        public void remove() {
            synchronized (BaseActor.this) {
            	if (count==0) {
    				throw new IllegalStateException("place is empty"); 
            	}
            	count--;
            	if (count==0) {
                	turnOff();
            	}
            }
        }

		protected boolean isEmpty() {
			return count==0;
		}
    }

    protected abstract class BasePort<T> extends Pin implements Port<T> {

        @Override
        public void send(T token) {
            synchronized (BaseActor.this) {
            	add(token);
            	turnOn();
            }
        }

        public T remove() {
            synchronized (BaseActor.this) {
            	T res =_remove();
            	if (isEmpty()) {
                	turnOff();
            	}
            	return res;
            }
        }

		protected abstract void add(T token);
		protected abstract boolean isEmpty();
		protected abstract T _remove();
    }

    /** A place for single token loaded with a reference of type <T>
     */
    public class ScalarInput<T> extends BasePort<T> {
    	protected T operand=null;

		@Override
		protected void add(T token) {
			if (token==null) {
				throw new IllegalArgumentException("operand may not be null"); 
			}
			if (operand!=null) {
				throw new IllegalStateException("place is occupied already"); 
			}
			operand=token;
		}

		@Override
		protected boolean isEmpty() {
			return operand==null;
		}

		@Override
		protected T _remove() {
	        if (isEmpty() ) {
	            throw new NoSuchElementException();
	        }
			T res=operand;
			operand=null;
			return res;
		}

		public T get() {
			return operand;
		}
    }

    /** A Queue of tokens of type <T>
     */
    public class StreamInput<T extends Link> extends BasePort<T> implements StreamPort<T>{
    	protected LinkedQueue<T> queue=new LinkedQueue<T>();
    	protected volatile boolean closed=false;

		@Override
		protected void add(T token) {
			if (token==null) {
				throw new IllegalArgumentException("operand may not be null"); 
			}
			queue.add(token);
		}

		@Override
		protected boolean isEmpty() {
			return queue.isEmpty() && !closed;
		}

		public boolean isClosed() {
			return closed;
		}

		@Override
		protected T _remove() {
			return queue.poll(); // may be null
		}

		@Override
		public void close() {
            synchronized (BaseActor.this) {
    			closed=true;
            	turnOn();
            }
		}
    }

    /**
     * 
     * This pin carries demand(s) of the result.
     * @param <R>  type of result
     */
    public class Demand<R> extends Pin implements Port<R>{
        private Connector<R> connector=new Connector<R>();

        /** indicates a demand
         */
        public void connect(Port<R> sink) {
            synchronized (BaseActor.this) {
            	connector.connect(sink);
            	turnOn();
            }
    	}

    	public void connect(StreamPort<R> sink) {
            synchronized (BaseActor.this) {
                synchronized (BaseActor.this) {
                	connector.connect(sink);
                	turnOn();
                }
            }
    	}

    	public void connect(Port<R>... sinks) {
            synchronized (BaseActor.this) {
            	connector.connect(sinks);
            	turnOn();
            }
    	}

    	/** satisfy demand(s)
    	 */
    	@Override
		public void send(R m) {
			connector.send(m);
		}

    }
    

}