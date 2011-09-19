package com.github.rfq.dffw.core;

import com.github.rfq.dffw.core.Link;

public class Request<R> extends Link {
	public Port<R> callback;
	
	public Request() {
	}
	
	public Request(Port<R> callback) {
		this.callback=callback;
	}
	
	public void reply(R result) {
		if (callback==null) return;
		callback.send(result);
	}
}
