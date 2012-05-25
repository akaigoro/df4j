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
 * 
 * @param <R>  type of result
 */
public class Connector<R> implements Port<R> {
    private Port<R> request;
    private ArrayList<Port<R>> requests = new ArrayList<Port<R>>();
    private Streamer<R> streamer;

	public void connect(Port<R> sink) {
		if (requests==null) {
			if (request==null) {
				request=sink;
				return;
			}
			requests = new ArrayList<Port<R>>();
			requests.add(request);
			request=null;
		}
		requests.add(sink);
	}

	public void connect(StreamPort<R> sink) {
		if (streamer==null) {
			streamer=new Streamer<R>();
			connect(streamer);
		}
		streamer.connect(sink);
	}

	public void connect(Port<R>... sinks) {
		for (Port<R> sink: sinks) {
			if (sink instanceof StreamPort) {
				connect((StreamPort<R>)sink);
			} else {
				connect(sink);				
			}			
		}
	}

	@Override
	public void send(R m) {
		for (Port<R> out: requests) {
			out.send(m);
		}
		requests.clear();
	}

	static class Streamer<R> implements Port<R> {
	    private StreamPort<R> request;

		public void connect(StreamPort<R> sink) {
			if (request==null) {
				request=sink;
				return;
			}
			StreamConnector<R> replicator;
			if (request instanceof StreamConnector) {
				replicator=(StreamConnector<R>) request;
			} else {
				replicator=new StreamConnector<R>();
				replicator.connect(request);
				request=replicator;
			}
			replicator.connect(sink);
		}

		@Override
		public void send(R m) {
			request.send(m);
			request.close();
			request=null;
		}
	}
}
