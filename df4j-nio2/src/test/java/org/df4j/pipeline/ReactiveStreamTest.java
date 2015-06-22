package org.df4j.pipeline;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class ReactiveStreamTest {
    static class ProducerSem extends ReactiveActor {
        BaseReactiveStreamOutput<Integer> output = new ReactiveSemStreamOutput<>();
        int cnt=0;

        @Override
        protected void act() throws Exception {
            System.out.println("produced:"+cnt);
            output.post(cnt);
            cnt++;
            if (cnt==10) {
                System.out.println("closing");
                output.close();
                stop();
            }
        }

    }

    static class Producer extends ReactiveActor {
        BaseReactiveStreamOutput<Integer> output = new ReactiveStreamOutput<>();
        int cnt=0;

        @Override
        protected void act() throws Exception {
            System.out.println("produced:"+cnt);
            output.post(cnt);
            cnt++;
            if (cnt==10) {
                System.out.println("closing");
                output.close();
                stop();
            }
        }

    }

    static class Consumer extends ReactiveActor {
        CountDownLatch fin=new CountDownLatch(1);
        ReactiveStreamInput<Integer> input = new ReactiveStreamInput<Integer>();

        @Override
        protected void act() throws Exception {
            Integer t=input.get();
            System.out.println("   consumed: "+t);
            if (t==null) {
                fin.countDown();
            } else {
                input.takeBack(t);
            }
        }

    }

    @Test
    public void testSem() throws InterruptedException {
        ProducerSem producer = new ProducerSem();
        Consumer consumer = new Consumer();
        producer.output.connect(consumer.input);
        producer.output.request(5);
        consumer.fin.await();
    }

    @Test
    public void test() throws InterruptedException {
        Producer producer = new Producer();
        Consumer consumer = new Consumer();
        producer.output.connect(consumer.input);
        for (int k=0; k<5; k++) {
            producer.output.takeBack(k);
        }
        consumer.fin.await();
    }

}
