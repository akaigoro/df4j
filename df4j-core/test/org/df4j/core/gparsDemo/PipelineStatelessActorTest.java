// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.df4j.core.gparsDemo;

import java.util.concurrent.ExecutionException;

import org.df4j.core.DFContext;
import org.df4j.core.actor.Actor1;
import org.df4j.core.func.Promise;
import org.junit.Test;

/**
 * test taken from GPars - Groovy Parallel Systems
 * org.codehaus.gpars.javademo.benchmark;
 * @author of original test  Jiri Mares, Vaclav Pech
 * @author of the ported test Alexei Kaigorodov
 */
public class PipelineStatelessActorTest {
    @Test
    public void testActor() throws InterruptedException, ExecutionException {
        DFContext.setFixedThreadPool(4);

        final StatefulDynamicDispatchActor writer = new DownloadStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor indexer = new IndexStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor downloader = new WriteStatefulDynamicDispatchActor();

        downloader.follower = indexer;
        indexer.follower = writer;
/*
        long t01 = System.currentTimeMillis();
        for (long i=0; i < 1000000L; i++) {
            downloader.handleMessage(indexer.handleMessage(writer.handleMessage("Requested " + i)));
        }        
        long t02 = System.currentTimeMillis();
        System.out.println("methods:"+(t02 - t01));
*/
        long t1 = System.currentTimeMillis();
        for (long i=0; i < 1000000L; i++) {
            downloader.post("Requested " + i);
        }
        downloader.close();
        downloader.join();
        indexer.join();
        writer.join();
        long t2 = System.currentTimeMillis();
        System.out.println("actors:"+(t2 - t1));
    }

}

abstract class StatefulDynamicDispatchActor extends Actor1<String> {
    Actor1<String> follower;
    Promise<Void> end=new Promise<Void>(); 
    
    abstract String handleMessage(String message);

    @Override
    protected void act(String message) throws Exception {
        if (follower != null) follower.post(handleMessage(message));
    }

    @Override
    protected void complete() throws Exception {
        if (follower != null) follower.close();
        end.post(null);
    }

    public void join() throws InterruptedException, ExecutionException {
        end.get();
    }

}

final class DownloadStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    @Override
    String handleMessage(final String message) {
        return message.replaceFirst("Requested ", "Downloaded ");
    }
}

final class IndexStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    @Override
    String handleMessage(final String message) {
        return message.replaceFirst("Downloaded ", "Indexed ");
    }
}

final class WriteStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    @Override
    String handleMessage(final String message) {
        return message.replaceFirst("Indexed ", "Wrote ");
    }
}
