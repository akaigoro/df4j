package com.github.rfqu.df4j.nio.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;

public  class ConnectionTest {
    static final int BUF_SIZE = 128;
//    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 8007);

    AsyncChannelFactory channelFactory;
    AsyncServerSocketChannel assc;
    
    @Before
    public void init() throws IOException {
    	channelFactory=AsyncChannelFactory.getCurrentAsyncChannelFactory();
        assc = channelFactory.newAsyncServerSocketChannel();
        assc.bind(local9990);
    }

    @After
    public void close() {
    	assc.close();
    }
    
	AsyncSocketChannel serverConn;
    AsyncSocketChannel clientConn;
    
    /**
     * send a message from client to server 
     */
    @Test
    public void smokeIOTest() throws Exception {
    	smokeTest1(); // open connections
    	
    	MyRequest srequest=new MyRequest();
		serverConn.read(srequest);
		MyRequest crequest=new MyRequest();
    	int value = 123455;
		crequest.buff.putInt(value);
    	clientConn.write(crequest);

    	srequest.get();
    	int value2 = srequest.buff.getInt();
    	assertEquals(value, value2);
    }
    
    /**
     * send a message from server to client 
     */
    @Test
    public void smokeIOTest1() throws Exception {
    	smokeTest1(); // open connections
    	
    	MyRequest crequest=new MyRequest();
    	clientConn.read(crequest);
		MyRequest srequest=new MyRequest();
    	int value = 123455;
		srequest.buff.putInt(value);
    	serverConn.write(srequest);

    	crequest.get();
    	int value2 = crequest.buff.getInt();
    	assertEquals(value, value2);
    }
    
    /**
     * send 2 messages in both directions simultaneousely 
     */
    @Test
    public void smokeIOTest2() throws Exception {
    	smokeTest1(); // open connections
    	int valueCS= 123455;
    	int valueSC = 0x12345;
    	
    	MyRequest crequestR=new MyRequest();
    	MyRequest crequestW=new MyRequest();
    	clientConn.read(crequestR);
		crequestW.buff.putInt(valueCS);
		clientConn.write(crequestW);

    	MyRequest srequestR=new MyRequest();
    	MyRequest srequestW=new MyRequest();
		srequestW.buff.putInt(valueSC);
    	serverConn.write(srequestW);
    	serverConn.read(srequestR);

    	crequestR.get();
    	int valueSC2 = crequestR.buff.getInt();
    	assertEquals(valueSC, valueSC2);

    	srequestR.get();
    	int valueCS2 = srequestR.buff.getInt();
    	assertEquals(valueCS, valueCS2);
    }
    
    /**
     * open connections on both sides, server first
     */
    @Test
    public void smokeTest1() throws Exception {
    	ListenableFuture<AsyncSocketChannel> connectionEvent = assc.accept();
    	clientConn=channelFactory.newAsyncSocketChannel();
        clientConn.connect(local9990);

        Thread.sleep(50);
        assertTrue(connectionEvent.isDone());
        assertTrue(clientConn.getConnEvent().isDone());
        serverConn=connectionEvent.get();
    }
    
    /**
     * open connections on both sides, client first
     */
    @Test
    public void smokeTest2() throws Exception {
    	AsyncSocketChannel asc=channelFactory.newAsyncSocketChannel();
        asc.connect(local9990);
    	ListenableFuture<AsyncSocketChannel> connectionEvent = assc.accept();
        Thread.sleep(50);
        assertTrue(connectionEvent.isDone());
        assertTrue(asc.getConnEvent().isDone());
    }

    static class MyRequest extends SocketIORequest<MyRequest> {
        ByteBuffer buff;
        
        public MyRequest() {
            super(ByteBuffer.allocate(BUF_SIZE));
            buff=super.getBuffer();
        }
    
    }
}