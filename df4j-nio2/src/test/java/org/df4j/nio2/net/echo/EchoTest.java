package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Dataflow;
import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public  class EchoTest {
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    Dataflow serverDataflow;
    Dataflow clientDataflow;
    EchoServer echoServer;

    @Before
    public synchronized void init() throws IOException {
        serverDataflow = new Dataflow();
        clientDataflow = new Dataflow();
        echoServer = new EchoServer(serverDataflow, local9990, 2);
        echoServer.start();
    }

    @After
    public synchronized void deinit() throws InterruptedException {
        echoServer.stop();
    }

    public void ClientTest_1(int total) throws IOException, InterruptedException {
        EchoClient client = new EchoClient(clientDataflow, local9990, total);
        client.start();
        boolean clfinised = clientDataflow.blockingAwait(1000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(clfinised);
        Assert.assertEquals(0, client.count);
        echoServer.stop();
        boolean sfinised = serverDataflow.blockingAwait(1, TimeUnit.MILLISECONDS);
        Assert.assertTrue(sfinised);
    }

    @Test
    public void ClientTest_1_1() throws IOException, InterruptedException {
        ClientTest_1(1);
    }

    @Test
    public void ClientTest_1_4() throws IOException, InterruptedException {
        ClientTest_1(4);
    }

    @Test
    public void ClientTest_3() throws IOException, InterruptedException {
        EchoClient client1 = new EchoClient(clientDataflow, local9990, 1);
        client1.start();
        EchoClient client2 = new EchoClient(clientDataflow, local9990, 2);
        client2.start();
        EchoClient client3 = new EchoClient(clientDataflow, local9990, 2);
        client3.start();
        boolean finised = clientDataflow.blockingAwait(1500, TimeUnit.MILLISECONDS);
        Assert.assertTrue(finised);
        Assert.assertEquals(0, client1.count);
        Assert.assertEquals(0, client2.count);
        echoServer.stop();
        boolean sfinised = serverDataflow.blockingAwait(1, TimeUnit.MILLISECONDS);
        Assert.assertTrue(sfinised);
    }

    @Test
    public void ClientTest_4x4() throws IOException, InterruptedException {
        ArrayList<EchoClient> clients = new ArrayList<>();
        for (int k=0; k<5; k++)  {
            EchoClient client = new EchoClient(clientDataflow, local9990, k+1);
            client.start();
            clients.add(client);
        }
        boolean finised = clientDataflow.blockingAwait(1, TimeUnit.SECONDS);
        Assert.assertTrue(finised);
        echoServer.stop();
        boolean sfinised = serverDataflow.blockingAwait(1, TimeUnit.MILLISECONDS);
        Assert.assertTrue(sfinised);
    }

}