package org.df4j.nio2.net.echo;

import org.df4j.core.communicator.ScalarResult;
import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * ConnectionManager => AsyncServerSocketChannel -> ServerConnection -|
 *     ^                                                             v
 *     |<------------------------------------------------------------|                                                             |
 */
public  class EchoTest {
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    static EchoServer echoServer;

    @BeforeClass
    public static synchronized void init() throws IOException {
        if (echoServer ==null) {
            echoServer = new EchoServer(local9990,2);
            echoServer.awake();
        }
    }

    @AfterClass
    public static synchronized void close() {
        if (echoServer !=null) {
            echoServer.close();
            echoServer = null;
        }
    }

    @Test
    public void ClientTest_1() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        EchoClient client = new EchoClient(local9990, 1);
        client.start();
        client.blockingAwait(1, TimeUnit.SECONDS);
    }

    @Test
    public void ClientTest_4x4() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ArrayList<EchoClient> clients = new ArrayList<>();
        for (int k=0; k<4; k++)  {
            EchoClient client = new EchoClient(local9990, k+1);
            client.start();
            clients.add(client);
        }
        for (EchoClient client: clients) {
            client.blockingAwait(2, TimeUnit.SECONDS);
        }
    }

}