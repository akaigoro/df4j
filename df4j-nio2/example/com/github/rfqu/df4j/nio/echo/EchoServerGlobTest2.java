package com.github.rfqu.df4j.nio.echo;

import com.github.rfqu.df4j.nio.AsyncChannelFactory2;
import com.github.rfqu.df4j.nio.echo.EchoServerGlobTest;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched as an application
 */
public class EchoServerGlobTest2 extends EchoServerGlobTest {

    public EchoServerGlobTest2() {
        super(new AsyncChannelFactory2());
    }

    public EchoServerGlobTest2(AsyncChannelFactory2 asyncChannelFactory2) {
        super(asyncChannelFactory2);
    }

    public static void main(String[] args) throws Exception {
        EchoServerGlobTest2 t=new EchoServerGlobTest2();
        t.run(args);
    }
}