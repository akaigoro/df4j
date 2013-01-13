package com.github.rfqu.df4j.nio2.echo;

import com.github.rfqu.df4j.test.AsyncChannelFactory;
import com.github.rfqu.df4j.test.AsyncChannelFactory1;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched as an application
 */
public class EchoServerGlobTest1 extends EchoServerGlobTest {

    public EchoServerGlobTest1(AsyncChannelFactory asyncChannelFactory) {
        super(asyncChannelFactory);
    }

    public static void main(String[] args) throws Exception {
        EchoServerGlobTest1 t=new EchoServerGlobTest1(new AsyncChannelFactory1());
        t.run(args);
    }
}