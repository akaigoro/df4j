/**
 * 
 */
package com.github.rfq.dffw.util;

import com.github.rfq.dffw.core.Port;
import com.github.rfq.dffw.core.Task;

public abstract class BinaryOp<T> extends Task {
    public Inp<T> p1=new Inp<T>(); 
    public Inp<T> p2=new Inp<T>();
    public DataSource<T> res = new  DataSource<T>();
    
	public class Inp<TP> implements Port<TP> {
        protected boolean opndready=false;
        TP operand;

		@Override
		public BinaryOp<T> send(TP v) {
	        synchronized(this) {
	            operand=v;
	            opndready=true;
	            if (!(p1.opndready && p2.opndready)) {
	                return BinaryOp.this;
	            }
	        }
	        fire();
            return BinaryOp.this;
        }
	}

	@Override
	public void run() {
		res.send(call(p1.operand, p2.operand));
	}

	abstract protected T call(T opnd, T opnd2);
	
}