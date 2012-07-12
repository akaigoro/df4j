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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 * May connect actors.
 * 
 * @param <T>  type of result
 */
public class Promise<T> implements Port<T> {
	protected volatile boolean _hasValue;
    protected T value;
    protected Port<T> listener;
    
	public void addListener(Port<T> sink) {
	    checkReady:
		synchronized (this) {
		    if (_hasValue) {
		        break checkReady;
		    }
            if (listener == null) {
                listener = sink;
                return;
            }
            Listeners proxy;
            if (listener instanceof Promise.Listeners) {
                proxy = (Listeners) listener;
            } else {
                proxy = new Listeners();
                proxy.addListener(listener);
                listener = proxy;
            }
            proxy.addListener(sink);
            return;
        }
	    sink.send(value);
	}

	public void add(Port<T>... sinks) {
		for (Port<T> sink: sinks) {
			addListener(sink);				
		}
	}

	@Override
	public void send(T m) {
        if (_hasValue) {
            throw new IllegalStateException("value set already");
        }
		synchronized (this) {
		    _hasValue=true;
		    value=m;
            if (listener == null) {
                return;
            }
        }
        listener.send(m);
	}

    public boolean hasValue() {
        return _hasValue;
    }
    
    public T get() {
        if (!_hasValue) {
            throw new IllegalStateException("value not set");
        }
        return value;
    }
    
	private class Listeners implements Port<T>{
	    private ConcurrentLinkedQueue<Port<T>> listeners = new ConcurrentLinkedQueue<Port<T>>();

		void addListener(Port<T> listener) {
			listeners.add(listener);
		}

		@Override
		public void send(T m) {
			for (;;) {
			    Port<T> out=listeners.poll();
			    if (out==null) return;
				out.send(m);
			}
		}
	}
}
