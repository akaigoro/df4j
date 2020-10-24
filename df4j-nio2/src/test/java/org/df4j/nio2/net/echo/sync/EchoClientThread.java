package org.df4j.nio2.net.echo.sync;

import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EchoClientThread extends Thread {
    private final Logger LOG = LoggerFactory.getLogger(this);
    public int count;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public EchoClientThread(InetSocketAddress socketAddress, int total) throws IOException {
        this.clientSocket = new Socket(socketAddress.getHostName(), socketAddress.getPort());
        this.count=total;
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public void run() {
        LOG.info("client thread started");
        try {
            do {
                String msg = "hi there " + count;
                out.println(msg);
                out.flush();
                LOG.info("client sent:"+msg);
                String inputLine = in.readLine();
                LOG.info("client received:"+inputLine);
            } while  (--count > 0);
            clientSocket.close();
        } catch (IOException e) {
            LOG.debug(e.getMessage());
        } finally {
            LOG.info("client thread stopped");
        }
    }
}
