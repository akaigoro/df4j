package com.github.rfqu.df4j.nio.tutorial;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

public class DataLoaderTest {

    static final InetSocketAddress local9990
           = new InetSocketAddress("localhost", 8078);

    DataServer ds;
    AsyncSocketChannel asc;

    /**
     * start server; 
     * open client's AsyncSocketChannel asc
     */
    @Before
    public void init() throws IOException {
        ds = new DataServer(local9990);
        ds.start(2);

        AsyncChannelFactory channelFactory = AsyncChannelFactory.getCurrentAsyncChannelFactory();
        asc = channelFactory.newAsyncSocketChannel();
        asc.connect(local9990);
    }
    
    @After
    public void deinit() {
        if (asc!=null) {
            asc.close();
        }
        if (ds!=null) {
            ds.close();
        }
    }

    /**
     * Loads 2 portions of data
     */
    @Test
    public void testLoad2() throws IOException, InterruptedException, ExecutionException {
        DataLoader loader = new DataLoader(asc);
        int[] data = loader.read().get();
        // TODO check
        int size=data[0];
        assertEquals(data.length, size);
        for (int k=0; k<size; k++) {
            assertEquals(k, data[k+1]);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        DataLoaderTest dataLoader = new DataLoaderTest();
        dataLoader.init();
        dataLoader.testLoad2();
    }

}
