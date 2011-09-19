package com.github.rfq.dffw.util;

import com.github.rfq.dffw.core.MessageQueue;
import com.github.rfq.dffw.core.Port;
import com.github.rfq.dffw.core.Request;

/**
 * 
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 *
 * @param <R> type of result
 */
public class DataSource<R> implements Port<R> {
	private MessageQueue<Request<R>> requests=new MessageQueue<Request<R>>();
    private volatile R result;

	public DataSource<R> send(R result) {
		this.result=result;
		for (;;) {
			Request<R> request;
	        synchronized (this) {
	            request = requests.poll();
	            if (request == null) {
	                return this;
	            }
	        }
	        try {
	        	request.reply(result);
	        } catch (Exception e) {
	            failure(request, e);
	        }
		}
	}

    /** handles the failure
     * 
     * @param message
     * @param e
     */
    protected void failure(Request<R> request, Exception e) {
        e.printStackTrace();
    }

    public <S extends Port<R>> S request(S sink) {
		if (result!=null) {
			sink.send(result);
		}
		synchronized (this) {
			if (result==null) {
				requests.enqueue(new Request<R>(sink));
				return sink;
			}
		}
		sink.send(result);
		return sink;
	}

    public void request(Request<R> request) {
		if (result!=null) {
			request.reply(result);
		}
		synchronized (this) {
			if (result==null) {
				requests.enqueue(request);
				return;
			}
		}
		request.reply(result);
	}

}
