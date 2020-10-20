package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Dataflow;
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
        echoServer = new EchoServer(serverDataflow, local9990);
        echoServer.start();
    }

    @After
    public synchronized void deinit() throws InterruptedException, IOException {
        if (echoServer != null) {
            echoServer.complete();
        }
    }

    public void ClientTest_1(int nc, int total) throws IOException, InterruptedException {
        ArrayList<EchoClient> clients = new ArrayList<>();
        for (int k = 0; k< nc; k++)  {
            EchoClient client = new EchoClient(clientDataflow, local9990, total);
            client.start();
            clients.add(client);
        }
        boolean finised = clientDataflow.await(1, TimeUnit.SECONDS);
        Assert.assertTrue(finised);
        for (EchoClient client: clients) {
            Assert.assertEquals(0, client.count);
        }
    }

    @Test
    public void ClientTest_1_1() throws IOException, InterruptedException {
        ClientTest_1(1, 1);
    }

    @Test
    public void ClientTest_1_4() throws IOException, InterruptedException {
        ClientTest_1(1, 4);
    }

    @Test
    public void ClientTest_4x1() throws IOException, InterruptedException {
        ClientTest_1(4, 1);
    }

    @Test
    public void ClientTest_4x4() throws IOException, InterruptedException {
        ClientTest_1(4, 4);
    }
}