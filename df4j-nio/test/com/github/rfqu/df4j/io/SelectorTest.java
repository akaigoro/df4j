package com.github.rfqu.df4j.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private static final Logger log = Logger.getLogger(SelectorTest.class);
    static final int BUF_SIZE = 4096;
    SimpleExecutorService executor;
    InetSocketAddress local9999;
    PrintStream out = System.out;
    PrintStream err = System.err;
    int clients = 2;
    int rounds = 3; // per client

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
        AsyncServerSocketChannel s = new AsyncServerSocketChannel(local9999);

        MessageSink sink = new MessageSink(clients);
        for (int k=0; k<clients; k++) {
            new ClientConnection(local9999, rounds, sink);
        }
        new ServerConnection(s);
        sink.await();
    }

    class ServerConnection extends AsyncSocketChannel implements Runnable {
        AsyncServerSocketChannel s;
        SocketIORequest request = new SocketIORequest(BUF_SIZE, false);

        public ServerConnection(AsyncServerSocketChannel s) throws IOException {
            this.s = s;
            super.connect(s);
        }

        @Override
        protected void connCompleted(SocketChannel channel) throws IOException {
            super.connCompleted(channel);
            Thread t = new Thread(this);
            t.setName(t.getName()+" ServerConnection");
            t.start();
        }

        @Override
        public void run() {
            Socket echoSocket =  super.channel.socket();
            PrintWriter outs;
            BufferedReader ins;

            try {
                outs = new PrintWriter(echoSocket.getOutputStream(), true);
                ins = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));

                for (;;) {
                    String msg = ins.readLine();
                    if (msg==null) break;
                    out.println("server received:" + msg);
                    outs.println(msg);
                }
                outs.close();
                ins.close();
                echoSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class ClientConnection extends AsyncSocketChannel implements Runnable {
        long rounds;
        MessageSink sink;
        String msg="Hi there";

        public ClientConnection(InetSocketAddress addr, long rounds, MessageSink sink) throws IOException {
            this.rounds = rounds;
            this.sink = sink;
            connect(addr);
            Thread t = new Thread(this);
            t.setName(t.getName()+" ClientConnection");
            t.start();
        }

        public void run() {
            Socket echoSocket =  super.channel.socket();
            PrintWriter outs;
            BufferedReader ins;

            try {
                outs = new PrintWriter(echoSocket.getOutputStream(), true);
                ins = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
                
                for (int i = 0; i < rounds; i++) {
                    outs.println(msg);
                    String reply = ins.readLine();
                    out.println("client received:" + reply);
                    sink.send(reply);
                    if (reply==null) {
                        err.println("unexpected null");
                    }
                }
                outs.close();
                ins.close();
                echoSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        SelectorTest t = new SelectorTest();
        t.init();
        t.testServerSocket();
    }
}
