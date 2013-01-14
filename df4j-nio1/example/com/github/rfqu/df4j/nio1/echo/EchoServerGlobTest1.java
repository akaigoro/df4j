package com.github.rfqu.df4j.nio1.echo;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio1.AsyncChannelFactory1;
import com.github.rfqu.df4j.nio2.echo.EchoServerGlobTest;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched as an application
 */
public class EchoServerGlobTest1 extends EchoServerGlobTest {

    public EchoServerGlobTest1() {
        super(new AsyncChannelFactory1());
    }

    public EchoServerGlobTest1(AsyncChannelFactory asyncChannelFactory) {
        super(asyncChannelFactory);
    }

    public static void main(String[] args) throws Exception {
        EchoServerGlobTest1 t=new EchoServerGlobTest1();
        t.run(args);
    }
}