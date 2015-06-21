package org.df4j.pipeline.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.df4j.pipeline.core.TransformerNode;
import org.df4j.pipeline.df4j.core.StreamPort;

public class String2ByteBuf extends TransformerNode<String, ByteBuffer>
{
    //----------------------- Source

    /** there output messages go */
    protected StreamPort<ByteBuffer> sinkPort;
    
    protected final String charset;
    
    public String2ByteBuf() {
    	this("UTF8");
    }
    
    public String2ByteBuf(String charset) {
    	this.charset = charset;
    }
    
    public void setSinkPort(StreamPort<ByteBuffer> sinkPort) {
        this.sinkPort=sinkPort;
    }
    
    public StreamPort<ByteBuffer> getReturnPort() {
        return null; // no return required
    }

	public void post(String s) {
		try {
			byte[] data = s.getBytes(charset);
			ByteBuffer buf=ByteBuffer.wrap(data);
	        sinkPort.post(buf);
		} catch (UnsupportedEncodingException e) {
			// cannot happen
			throw new RuntimeException(e);
		}
    }

    public void close() {
        sinkPort.close();
    }

    public void postFailure(Throwable exc) {
        context.postFailure(exc);
    }
    
    //----------------- backend

    @Override
    protected void act(String message) throws Exception {
        String str=input.get();
        try {
            byte[] array=str.getBytes(charset);
            ByteBuffer buf=ByteBuffer.wrap(array);
            sinkPort.post(buf);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void complete() throws Exception {
        sinkPort.close();
    }
}
