package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;

public abstract class AsyncChannelFactory {
    public abstract AsyncServerSocketChannel newAsyncServerSocketChannel(SocketAddress addr, Callback<AsyncSocketChannel> callback) throws IOException;

    public abstract AsyncSocketChannel newAsyncSocketChannel(SocketAddress addr) throws IOException;

    //--------------------- context
    
    /** implementations */
    static final String[] factoryClassNames={"com.github.rfqu.df4j.nio.AsyncChannelFactory2",
            "com.github.rfqu.df4j.nio.AsyncChannelFactory1"
    };
    
    private static final ItemKey<AsyncChannelFactory> AsyncChannelFactorydKey
        = DFContext.getCurrentContext().new ItemKey<AsyncChannelFactory>()
    {
        @Override
        protected AsyncChannelFactory initialValue(DFContext context) {
            for (String factoryClassName: factoryClassNames) {
                Class<?> factoryClass;
                try {
                    factoryClass = Class.forName(factoryClassName);
                } catch (ClassNotFoundException e) {
                    continue;
                }
                try {
                    return (AsyncChannelFactory) factoryClass.newInstance();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }
        
    };
    
    public static AsyncChannelFactory getCurrentAsyncChannelFactory() {
        return AsyncChannelFactorydKey.get();
    }
 }