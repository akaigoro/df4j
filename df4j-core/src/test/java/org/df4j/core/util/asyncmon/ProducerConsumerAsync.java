package org.df4j.core.util.asyncmon;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.permitscalar.ScalarPermitSubscriber;
import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor1;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProducerConsumerAsync extends AllOf {

    class BlockingQ<T> extends AsyncObject {
        private final int maxItems;
        private final ArrayDeque<T> buf;
        private int count = 0;

        public BlockingQ(int maxItems) {
            this.maxItems = maxItems;
            buf = new ArrayDeque(maxItems);
            CompletablePromise<?> scalarPublisher = super.asyncResult();
            registerAsyncDaemon(scalarPublisher);
        }

        public void put(T item, ScalarPermitSubscriber connector) {
            super.exec((AsyncMonitor monitor) -> {
                    if (count == maxItems) {
                        monitor.doWait();
                        return;
                    }
                    buf.add(item);
                    count++;
                    monitor.doNotifyAll();
                    connector.release();
                }
            );
        }

        public CriticalSection take(Subscriber<T> connector) {
            CriticalSection criticalSection = (AsyncMonitor monitor) -> {
                if (count == 0) {
                    monitor.doWait();
                    return;
                }
                T item = buf.poll();
                count--;
                monitor.doNotifyAll();
                connector.onNext(item);
            };
            super.exec(criticalSection);
            return criticalSection;
        }
    }

    /** this is an implicit Actor, which restarts only after
     * blockingQ.put() finished
     *
     */
    class IntProducer extends AsyncAction {

        final BlockingQ<Integer> blockingQ;
        int k = 0;

        IntProducer(BlockingQ<Integer> blockingQ) {
            this.blockingQ = blockingQ;
            registerAsyncResult(asyncResult());
        }

        @Override
        public void runAction() {
            if (k < 10) {
                blockingQ.put(k, super::start);
                k++;
            } else {
                stop();
            }
        }
    }

    class IntConsumer extends Actor1<Integer> {
        final BlockingQ<Integer> blockingQ;
        int k = 0;

        IntConsumer(BlockingQ<Integer> blockingQ) {
            this.blockingQ = blockingQ;
            registerAsyncResult(asyncResult());
        }

        @Override
        public void start() {
            blockingQ.take(this);
            super.start();
        }

        @Override
        protected void runAction(Integer item) throws Exception {
            Assert.assertEquals(k, item.intValue());
            k++;
            if (k == 10) {
                stop();
            }
        }
    }

    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncProc.setThreadLocalExecutor(AsyncProc.currentThreadExec);
        BlockingQ blockingQ = new BlockingQ(5);
        IntProducer producer = new IntProducer(blockingQ);
        IntConsumer consumer = new IntConsumer(blockingQ);
        super.start(); // only after all components created
        producer.start();
        consumer.start();
        asyncResult().get(1, TimeUnit.SECONDS);
    }
}
