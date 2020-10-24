package org.df4j.nio2.net.echo.sync;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public  class EchoTest {
    static final Logger LOG = LoggerFactory.getLogger(EchoTest.class);
    static final int port = 5555;
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", port);
//    static final SocketAddress local9990 = new InetSocketAddress("52.20.16.20",30000);

    public void ClientTest_1(int nc, int total) throws IOException, InterruptedException {
        ArrayList<EchoClientThread> clients = new ArrayList<>();
        for (int k = 0; k< nc; k++)  {
            EchoClientThread client = new EchoClientThread(local9990, total);
            client.start();
            clients.add(client);
        }
        for (EchoClientThread client: clients) {
            client.join(100000);
            Assert.assertFalse(client.isAlive());
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