package com.github.rfqu.df4j.nio.test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.nio.AsyncChannelFactory2;
import com.github.rfqu.df4j.nio.test.LimitedServerTest;

public class LimitedServerTest2 extends LimitedServerTest {
    {asyncrSocketFactory=new AsyncChannelFactory2();
    }

    
    public static void main(String[] args) throws Exception {
    	DFContext.setCurrentExecutor(new ImmediateExecutor());
    	new LimitedServerTest2().smokeTest();
    }
    
}