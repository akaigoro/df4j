package org.df4j.pipeline.util;

import java.util.concurrent.LinkedBlockingQueue;
import org.df4j.pipeline.core.BoltBase;
import org.df4j.pipeline.core.Sink;
import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;

public class StringSink extends BoltBase implements Sink<String> {
    protected LinkedBlockingQueue<String> output=new LinkedBlockingQueue<String>();

    /**  here input messages arrive */
    protected StreamPort<String> myInput=new StreamPort<String>() {

		@Override
		public void post(String message) {
			try {
				output.put(message);
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isClosed() {
			return false;
		}
    	
    };

    @Override
    public StreamPort<String> getInputPort() {
        return myInput;
    }
    
    public LinkedBlockingQueue<String> getOutput() {
		return output;
	}

	public boolean isClosed() {
        return myInput.isClosed();
    }
 
	@Override
	public void setReturnPort(Port<String> returnPort) {
	}
}