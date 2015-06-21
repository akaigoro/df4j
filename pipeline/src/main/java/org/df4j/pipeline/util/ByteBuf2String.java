package org.df4j.pipeline.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import org.df4j.pipeline.core.TransformerNode;
import org.df4j.pipeline.df4j.core.Port;

public class ByteBuf2String extends TransformerNode<ByteBuffer, String> {

    public ByteBuf2String() {
    }

    public ByteBuf2String(Executor executor) {
        super(executor);
    }

	@Override
	public Port<String> getReturnPort() {
		return null; // Strings not reused, and not counted
	}
	
    //--------------- Dataflow backend
    
    StringBuilder sb=new StringBuilder();

    void formString() {
        String s=sb.toString();
        sinkPort.post(s);
        sb=new StringBuilder();
    }

    @Override
    protected void act(ByteBuffer inbuf) {
        byte[] bytes = inbuf.array();
        int size=inbuf.remaining();
        String bufString;
        try {
            bufString = new String(bytes, 0, size, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
        int fromIndex=0;
        for (;;) {
            int nlpos=bufString.indexOf('\n', fromIndex);
            if (nlpos==-1) {
                sb.append(bufString, fromIndex, bufString.length());
                break;
            }
            sb.append(bufString, fromIndex, nlpos);
            formString();
            fromIndex=nlpos+1;
        }
        free(inbuf);
    }

    @Override
    protected void complete() {
        formString();
    }
}