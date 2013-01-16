package com.github.rfqu.df4j.nio.echo;

/**
 *  Runs tests with {@EchoServer2} launched in the same JVM.
 *  
 *  To run as Junit tests, first run {@EchoServer1} or {@EchoServer2}
 *  as a java application manuually.
 *  
 *  When run as a java application, {@EchoServer1} is started from the
 *  {@link EchoServerGlobTest#main} method.
 */
public class EchoServerGlobTest1 extends EchoServerGlobTest {

    public static void main(String[] args) throws Exception {
        EchoServerGlobTest.main(args);
    }

}