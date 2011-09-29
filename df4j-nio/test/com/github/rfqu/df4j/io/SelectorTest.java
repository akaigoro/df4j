
package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.junit.Before;
import org.junit.Test;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.util.MessageSink;

/**
 * Requires Java 7.
 */
public class SelectorTest {
    private static final Logger log = Logger.getLogger( SelectorTest.class ); 
    SimpleExecutorService executor;
    InetSocketAddress local9999;
    PrintStream out=System.out;
    PrintStream err=System.err;
    int clients=2;
    int rounds = 4; // per client

    @Before
    public void init() {
        executor = new SimpleExecutorService();
        out.println("Using " + executor.getClass().getCanonicalName());
        local9999 = new InetSocketAddress("localhost", 9998);
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.ALL);
    }
    
    @Test
    public void testServerSocket() throws Exception {
        Task.setCurrentExecutor(executor);
        AsyncServerSocketChannel s=new AsyncServerSocketChannel(local9999);
        ServerConnection sc = new ServerConnection(s);

        MessageSink sink = new MessageSink(1);
        ClientConnection cc = new ClientConnection(local9999, 1, sink);
        sink.await();
    }

    public static void main(String[] args) throws Exception {
        SelectorTest t=new SelectorTest();
        t.init();
        t.testServerSocket();
    }
}
