package com.github.rfqu.df4j.nio2.echo;

import com.github.rfqu.df4j.test.AsyncChannelFactory1;

public class EchoServerLocTest1 extends EchoServerLocTest {
	
    public EchoServerLocTest1(EchoServerGlobTest gt) {
        super(gt);
    }

    public static void main(String[] args) throws Exception {
        EchoServerGlobTest1 gt=new EchoServerGlobTest1(new AsyncChannelFactory1());
        EchoServerLocTest1 t=new EchoServerLocTest1(gt);
        t.run(args);
    }

}
