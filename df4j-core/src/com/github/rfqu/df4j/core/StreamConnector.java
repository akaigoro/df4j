package com.github.rfqu.df4j.core;

import java.util.ArrayList;

/** Facility to replicate results to multiple sinks of type StreamPort.
 * @param <R> type of accepted messages
 */
class StreamConnector<R> implements StreamPort<R> {
    private StreamPort<R> request;
    private ArrayList<StreamPort<R>> requests;
    private Collector<R> collector;

	public void connect(Port<R[]> sink) {
		if (collector==null) {
			collector=new Collector<R>();
			connect(collector);
		}
		collector.connect(sink);
	}

	public void connect(StreamPort<R> sink) {
		if (requests==null) {
			if (request==null) {
				request=sink;
				return;
			}
			requests = new ArrayList<StreamPort<R>>();
			requests.add(request);
			request=null;
		}
		requests.add(sink);
	}

	public void connect(StreamPort<R>... sinks) {
		for (StreamPort<R> sink: sinks) {
			connect(sink);				
		}
	}

	public void connect(Port<R[]>... sinks) {
		for (Port<R[]> sink: sinks) {
			connect(sink);				
		}
	}

	@Override
	public void send(R m) {
		for (StreamPort<R> out: requests) {
			out.send(m);
		}
	}

	@Override
	public void close() {
		for (StreamPort<R> out: requests) {
			out.close();
		}
		requests.clear();
	}

	static class Collector<R> implements StreamPort<R> {
	    private Port<R[]> request;
	    private ArrayList<R> tokens = new ArrayList<R>();

		public void connect(Port<R[]> sink) {
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

		@SuppressWarnings("unchecked")
        @Override
		public void close() {
			request.send((R[]) tokens.toArray(new Object[tokens.size()]));
		}
	}
}