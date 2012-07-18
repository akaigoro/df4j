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
public class Promise<T> implements Callback<T> {
	protected volatile boolean _hasValue;
    protected T value;
    protected Throwable exc;
    protected Callback<T> listener;
    
	public void addListener(Callback<T> sink) {
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
	    if (exc!=null) {
	        sink.sendFailure(exc);
	    } else {
	        sink.send(value);
	    }
	}

	public void addListeners(Callback<T>... sinks) {
		for (Callback<T> sink: sinks) {
			addListener(sink);				
		}
	}

	@Override
	public void send(T m) {
        if (_hasValue) {
            throw new IllegalStateException("value set already");
        }
        Callback<T> listenerLoc;
		synchronized (this) {
		    _hasValue=true;
		    value=m;
            if (listener == null) {
                return;
            }
            listenerLoc=listener;
            listener=null;
        }
		listenerLoc.send(m);
	}

    @Override
    public void sendFailure(Throwable exc) {
        if (_hasValue) {
            throw new IllegalStateException("value set already");
        }
        Callback<T> listenerLoc;
        synchronized (this) {
            _hasValue=true;
            this.exc=exc;
            if (listener == null) {
                return;
            }
            listenerLoc=listener;
            listener=null;
        }
        listenerLoc.sendFailure(exc);  
    }

	private class Listeners implements Callback<T>{
	    private ConcurrentLinkedQueue<Callback<T>> listeners = new ConcurrentLinkedQueue<Callback<T>>();

		void addListener(Callback<T> listener) {
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

        @Override
        public void sendFailure(Throwable exc) {
            for (;;) {
                Callback<T> out=listeners.poll();
                if (out==null) return;
                out.sendFailure(exc);
            }
        }
	}
}
