package com.github.rfqu.df4j.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.Task;

public class SelectorTest {

    Selector selector;
    String listenHost;
    int listenPort=9998;
    InetSocketAddress listenAddr;

    @Before
    public void init() throws IOException {
        this.selector = Selector.open();
        listenHost = "localhost";
        listenAddr = new InetSocketAddress(listenHost, 9998);

    }

	@Test
	public void t1() throws IOException {
        selector.wakeup();
        int count=selector.select();
        assertEquals(0, count);
	}
    
	@Test
	public void t2() throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
		serverChannel.socket().bind(listenAddr);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        int count=selector.select();
        assertEquals(1, count);

        SocketChannel serverConn = serverChannel.accept();
	}
    
    public void run() {
        try {
            Socket clientConn = new Socket(listenHost, listenPort);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }
}
