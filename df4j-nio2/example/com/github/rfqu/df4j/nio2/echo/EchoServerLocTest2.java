package com.github.rfqu.df4j.nio2.echo;

import com.github.rfqu.df4j.nio2.AsyncChannelFactory2;

public class EchoServerLocTest2 extends EchoServerLocTest {
	
    public EchoServerLocTest2() {
        super(new EchoServerGlobTest2(new AsyncChannelFactory2()));
    }

    public static void main(String[] args) throws Exception {
        EchoServerLocTest2 t=new EchoServerLocTest2();
        t.run(args);
    }

}
