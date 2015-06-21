package com.github.rfqu.pipeline.net.test;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.io.net.AsyncSocketChannel;
import org.df4j.pipeline.util.ByteBuf2String;
import org.df4j.pipeline.util.String2ByteBuf;
import org.df4j.pipeline.util.StringSink;

/** 
 * socket connection divided in inbound pipeline:
 *    socket=>stringArrived(String)
 * and pipeline pipeline:
 *    write(String)=>socket
 */
public class ClientConnection {
    public static final int BUF_SIZE = 128;

    protected AsyncSocketChannel channel;
    Pipeline pipeline = new Pipeline();
    String2ByteBuf stringSource = new String2ByteBuf();
    StringSink stringSink = new StringSink();

    //------------- Sink
    
    public ClientConnection(AsyncSocketChannel channel) throws IOException {
        this.channel=channel;
        
        pipeline.setSource(channel.reader)
          .addTransformer(new ByteBuf2String())
          .setSink(stringSink)
          .setSource(stringSource)
          .setSink(channel.writer);
    }

    public void start() {
    	channel.reader.injectBuffers(2, BUF_SIZE);
    	pipeline.start();
   }

    public void close() {
        pipeline.close();
    }

    public void write(String message) {
        stringSource.post(message);
    }

    public LinkedBlockingQueue<String> getOutput() {
        return stringSink.getOutput();
    }

    public String read() throws InterruptedException {
        return stringSink.getOutput().take();
    }
    
}