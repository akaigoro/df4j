package org.df4j.nio2.net.echo;

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

    static ConnectionManager connectionManager;

    @BeforeClass
    public static synchronized void init() throws IOException {
        if (connectionManager==null) {
            connectionManager = new ConnectionManager(local9990,2);
            connectionManager.start();
        }
    }

    @AfterClass
    public static synchronized void close() {
        if (connectionManager!=null) {
            connectionManager.close();
            connectionManager = null;
        }
    }

    @Test
    public void ClientTest_1() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        EchoClient client = new EchoClient(local9990, 1);
        client.start();
        client.result.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void ClientTest_4x4() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ArrayList<EchoClient> clients = new ArrayList<>();
        for (int k=0; k<4; k++)  {
            EchoClient client = new EchoClient(local9990, 4);
            client.start();
            clients.add(client);
        }
        for (EchoClient client: clients) {
            client.result.get(2, TimeUnit.SECONDS);
        }
    }

}