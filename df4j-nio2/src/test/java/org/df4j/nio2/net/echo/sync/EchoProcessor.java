package org.df4j.nio2.net.echo.sync;

import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class EchoProcessor extends Thread {
    private final Logger LOG = LoggerFactory.getLogger(this);
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public EchoProcessor(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        LOG.info("service thread started");
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            for (; ; ) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                LOG.info("inputLine = " + inputLine);
                out.println(inputLine);
            }
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            LOG.debug(e.getMessage());
        } finally {
            LOG.info("service thread stopped");
        }
    }
}
