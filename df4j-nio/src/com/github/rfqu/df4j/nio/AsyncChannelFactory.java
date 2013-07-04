/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.core.DFContext.ItemKey;

public abstract class AsyncChannelFactory {
    public abstract AsyncServerSocketChannel newAsyncServerSocketChannel() throws IOException;

    /** creates client connection */
    public abstract AsyncSocketChannel newAsyncSocketChannel() throws IOException;

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