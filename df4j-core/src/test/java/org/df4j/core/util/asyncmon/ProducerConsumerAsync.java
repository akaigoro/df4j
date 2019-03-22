package org.df4j.core.util.asyncmon;

import org.df4j.core.boundconnector.permitstream.PermitSubscriber;
import org.reactivestreams.Subscriber;
import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor1;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProducerConsumerAsync extends AllOf {

    class NonBlockingQ<T> extends AsyncObject {
        private final int maxItems;
        private final ArrayDeque<T> buf;
        private int count = 0;

        public NonBlockingQ(int maxItems) {
            this.maxItems = maxItems;
            buf = new ArrayDeque(maxItems);
            CompletablePromise<?> scalarPublisher = super.asyncResult();
            registerAsyncDaemon(scalarPublisher);
        }

        public void put(T item, Runnable connector) {
            super.exec((AsyncMonitor monitor) -> {
                    if (count == maxItems) {
                        monitor.doWait();
                        return;
                    }
                    buf.add(item);
                    count++;
                    monitor.doNotifyAll();
                    connector.run();
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

        final NonBlockingQ<Integer> nonBlockingQ;
        int k = 0;

        IntProducer(NonBlockingQ<Integer> nonBlockingQ) {
            this.nonBlockingQ = nonBlockingQ;
            registerAsyncResult(asyncResult());
        }

        @Override
        public void runAction() {
            if (k < 10) {
                nonBlockingQ.put(k, super::start);
                k++;
            } else {
                stop();
            }
        }
    }

    class IntConsumer extends Actor1<Integer> {
        final NonBlockingQ<Integer> blockingQ;
        int k = 0;

        IntConsumer(NonBlockingQ<Integer> blockingQ) {
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
        NonBlockingQ blockingQ = new NonBlockingQ(5);
        IntProducer producer = new IntProducer(blockingQ);
        IntConsumer consumer = new IntConsumer(blockingQ);
        super.start(); // only after all components created
        producer.start();
        consumer.start();
        asyncResult().get(1, TimeUnit.SECONDS);
    }
}
