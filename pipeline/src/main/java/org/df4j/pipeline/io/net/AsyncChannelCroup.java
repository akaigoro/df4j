/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.df4j.pipeline.io.net;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import org.df4j.pipeline.df4j.core.DFContext;
import org.df4j.pipeline.df4j.core.DFContext.ItemKey;

/**
 * Provides access to the current {@link java.nio.channels.AsynchronousChannelGroup}
 * supported by the current {@link java.util.concurrent.Executor}.
 * @author rfqu
 */
public class AsyncChannelCroup {
    /**
     * sets current group as a thread-local variable
     * 
     * @param group
     */
    public static void setCurrentACGroup(AsynchronousChannelGroup group) {
        groupKey.set(group);
    }

    /**
     * retrieves current group as a thread-local variable
     */
    public static AsynchronousChannelGroup getCurrentACGroup() {
        return groupKey.get();
    }

    private static ItemKey<AsynchronousChannelGroup> groupKey 
        = DFContext.getCurrentContext().new ItemKey<AsynchronousChannelGroup>()
    {

        @Override
        protected AsynchronousChannelGroup initialValue(DFContext context) {
            ExecutorService service = context._getCurrentExecutorService();
            try {
                return AsynchronousChannelGroup.withThreadPool(service);
            } catch (IOException e) {
                return null;
            }
        }

    };
}
