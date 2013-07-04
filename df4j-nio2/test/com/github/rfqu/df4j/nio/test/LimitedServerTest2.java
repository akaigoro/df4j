package com.github.rfqu.df4j.nio.test;

import com.github.rfqu.df4j.nio.AsyncChannelFactory2;
import com.github.rfqu.df4j.nio.test.LimitedServerTest;

public class LimitedServerTest2 extends LimitedServerTest {
    {asyncrSocketFactory=new AsyncChannelFactory2();
    }
}