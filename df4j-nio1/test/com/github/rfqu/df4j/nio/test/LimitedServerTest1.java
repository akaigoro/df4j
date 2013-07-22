package com.github.rfqu.df4j.nio.test;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncChannelFactory1;
import com.github.rfqu.df4j.nio.test.LimitedServerTest;


public class LimitedServerTest1 extends LimitedServerTest {
    {
        AsyncChannelFactory.setCurrentAsyncChannelFactory(new AsyncChannelFactory1());
    }
}