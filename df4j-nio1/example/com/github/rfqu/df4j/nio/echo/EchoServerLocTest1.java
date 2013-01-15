package com.github.rfqu.df4j.nio.echo;

import com.github.rfqu.df4j.nio.AsyncChannelFactory1;
import com.github.rfqu.df4j.nio.echo.EchoServerLocTest;

public class EchoServerLocTest1 extends EchoServerLocTest {
	
    public EchoServerLocTest1() {
        super(new EchoServerGlobTest1(new AsyncChannelFactory1()));
    }

    public static void main(String[] args) throws Exception {
        EchoServerLocTest1 t=new EchoServerLocTest1();
        t.run(args);
    }

}
