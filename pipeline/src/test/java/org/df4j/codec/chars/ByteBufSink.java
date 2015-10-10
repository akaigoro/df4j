package org.df4j.codec.chars;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import org.df4j.pipeline.core.BoltBase;
import org.df4j.pipeline.core.Sink;
import org.df4j.pipeline.df4j.core.CompletableFuture;
import org.df4j.pipeline.df4j.core.ListenableFuture;
import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;

public class ByteBufSink extends BoltBase implements Sink<ByteBuffer> {
    
    /** there input messages return */
    protected Port<ByteBuffer> returnPort;

    @Override
    public void setReturnPort(Port<ByteBuffer> returnPort) {
        this.returnPort=returnPort;
    }

    CompletableFuture<byte[]> futt=new CompletableFuture<byte[]>();
    ByteArrayOutputStream bac=new ByteArrayOutputStream();

    StreamPort<ByteBuffer> myInput=new StreamPort<ByteBuffer>() {
        
        @Override
        public void post(ByteBuffer buf) {
            while (buf.hasRemaining()) {
                bac.write(buf.get());
            }
            returnPort.post(buf);
        }

        @Override
        public void close() {
            futt.post(bac.toByteArray());
        }

        @Override
        public boolean isClosed() {
            return futt.isDone();
        }
    };

    @Override
    public StreamPort<ByteBuffer> getInputPort() {
        return myInput;
    }

	public ListenableFuture<byte[]> getFuture() throws InterruptedException, ExecutionException {
		return futt;
	}
}