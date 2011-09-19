/**
 * 
 */
package com.github.rfq.dffw.util;

import com.github.rfq.dffw.core.Port;
import com.github.rfq.dffw.core.Task;

public abstract class UnaryOp<T, R> extends Task implements Port<T> {
    public DataSource<R> res = new  DataSource<R>();
    T opnd;

	@Override
	public UnaryOp<T, R> send(T v) {
        synchronized(this) {
            opnd=v;
        }
        fire();
        return this;
    }
	
	@Override
	public void run() {
		res.send(call(opnd));
	}

	abstract protected R call(T opnd2);
	
}