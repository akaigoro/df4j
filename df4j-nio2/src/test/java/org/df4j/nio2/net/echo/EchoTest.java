package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.util.Utils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public  class EchoTest {
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    static EchoServer echoServer;

    @BeforeClass
    public static synchronized void init() throws IOException {
        if (echoServer == null) {
            echoServer = new EchoServer(new Dataflow(), local9990);
            echoServer.start();
        }
    }

    @AfterClass
    public static synchronized void close() {
        if (echoServer != null) {
            echoServer.close();
            echoServer = null;
        }
    }

    @Test
    public void ClientTest_1() throws IOException, InterruptedException {
        Dataflow dataflow = new Dataflow();
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
        dataflow.setExecutor(executor); // for debug
        EchoClient client = new EchoClient(dataflow, local9990, 1);
        client.start();
        executor.executeAll(50);
        boolean finised = dataflow.blockingAwait(1, TimeUnit.SECONDS);
        Assert.assertTrue(finised);
        Assert.assertEquals(client.total, client.count);
    }

    @Test
    public void ClientTest_4x4() throws IOException, InterruptedException {
        Dataflow dataflow = new Dataflow();
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
        dataflow.setExecutor(executor); // for debug
        ArrayList<EchoClient> clients = new ArrayList<>();
        for (int k=0; k<4; k++)  {
            EchoClient client = new EchoClient(dataflow, local9990, k+1);
            client.start();
            clients.add(client);
        }
        executor.executeAll(50);
        boolean finised = dataflow.blockingAwait(1, TimeUnit.SECONDS);
        Assert.assertTrue(finised);
    }

}