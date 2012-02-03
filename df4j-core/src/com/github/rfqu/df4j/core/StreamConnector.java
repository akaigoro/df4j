package com.github.rfqu.df4j.core;

import java.util.ArrayList;

class StreamConnector<R> implements OutStreamPort<R> {
    private OutStreamPort<R> request;
    private ArrayList<OutStreamPort<R>> requests;
    private StreamConnector.Collector<R> collector;

	public void connect(OutPort<R[]> sink) {
		if (collector==null) {
			collector=new StreamConnector.Collector<R>();
			connect(collector);
		}
		collector.connect(sink);
	}

	public void connect(OutStreamPort<R> sink) {
		if (requests==null) {
			if (request==null) {
				request=sink;
				return;
			}
			requests = new ArrayList<OutStreamPort<R>>();
			requests.add(request);
			request=null;
		}
		requests.add(sink);
	}

	public void connect(OutStreamPort<R>... sinks) {
		for (OutStreamPort<R> sink: sinks) {
			connect(sink);				
		}
	}

	public void connect(OutPort<R[]>... sinks) {
		for (OutPort<R[]> sink: sinks) {
			connect(sink);				
		}
	}

	@Override
	public void send(R m) {
		for (OutStreamPort<R> out: requests) {
			out.send(m);
		}
	}

	@Override
	public void close() {
		for (OutStreamPort<R> out: requests) {
			out.close();
		}
		requests.clear();
	}

	static class Collector<R> implements OutStreamPort<R> {
	    private OutPort<R[]> request;
	    private ArrayList<R> tokens = new ArrayList<R>();

		public void connect(OutPort<R[]> sink) {
			if (request==null) {
				request=sink;
				return;
			}
			Connector<R[]> replicator;
			if (request instanceof Connector) {
				replicator=(Connector<R[]>) request;
			} else {
				replicator=new Connector<R[]>();
				replicator.connect(request);
				request=replicator;
			}
			replicator.connect(sink);
		}

		@Override
		public void send(R m) {
			tokens.add(m);
		}

		@Override
		public void close() {
			request.send((R[]) tokens.toArray(new Object[tokens.size()]));
		}
	}
}