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
import java.util.ArrayList;

/**
 * 
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 * Distributes received value among listeners.
 * Value (or failure) can only be assigned once. It is then saved, and 
 * listeners connected after the assignment still would receive it.
 * May connect actors.
 * <p>Promise plays the same role as {@link java.util.concurrent.Future},
 * but the result is sent to ports, registered as listeners using {@link #addListener}.
 * Registration can happen at any time, before or after the result is computed.
 * 
 * @param <T>  type of result
 */
public class CallbackPromise<T> implements Callback<T>, Promise<T> {
	protected boolean _hasValue;
    protected T value;
    protected Throwable exc;
    protected Callback<T> listener;
    
    public CallbackPromise() {
    }

    public CallbackPromise(Callback<T> firstListener) {
        this.listener = firstListener;
    }

    @Override
	public Promise<T> addListener(Callback<T> sink) {
		synchronized (this) {
		    if (!_hasValue) {
	            if (listener == null) {
	                listener = sink;
	                return this;
	            }
	            if (listener instanceof CallbackPromise.Listeners) {
	                ((Listeners) listener).addListener(sink);
	            } else {
	                Listeners listeners = new Listeners();
	                listeners.addListener(listener);
	                listeners.addListener(sink);
	                listener=listeners;
	            }
	            return this;
		    }
        }
	    if (exc!=null) {
	        sink.postFailure(exc);
	    } else {
	        sink.post(value);
	    }
        return this;
	}

	@Override
	public void post(T m) {
        Callback<T> listenerLoc;
		synchronized (this) {
	        if (_hasValue) {
	            Object v=this.exc!=null?this.exc:value;
	            throw new IllegalStateException("value set already: "+v);
	        }
		    _hasValue=true;
		    value=m;
            if (listener == null) {
                return;
            }
            listenerLoc=listener;
            listener=null;
        }
		listenerLoc.post(m);
	}

    @Override
    public void postFailure(Throwable exc) {
        Callback<T> listenerLoc;
        synchronized (this) {
            if (_hasValue) {
                Object v=this.exc!=null?this.exc:value;
                throw new IllegalStateException("value set already: "+v);
            }
            _hasValue=true;
            this.exc=exc;
            if (listener == null) {
                return;
            }
            listenerLoc=listener;
            listener=null;
        }
        listenerLoc.postFailure(exc);  
    }

	private class Listeners implements Callback<T>{
	    private ArrayList<Callback<T>> listeners = new ArrayList<Callback<T>>();

		void addListener(Callback<T> listener) {
			listeners.add(listener);
		}

		@Override
		public void post(T m) {
			for (int k=0; k<listeners.size(); k++) {
			    listeners.get(k).post(m);
			}
		}

        @Override
        public void postFailure(Throwable exc) {
            for (int k=0; k<listeners.size(); k++) {
                listeners.get(k).postFailure(exc);
            }
        }
	}
}
