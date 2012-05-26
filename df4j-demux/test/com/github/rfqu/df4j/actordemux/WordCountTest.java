package com.github.rfqu.df4j.actordemux;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.rfqu.df4j.core.PortFuture;


public class WordCountTest {

    static class Record implements Delegate<Action<Record>>{
        long counter;

        @Override
        public void act(Action<Record> message) {
            message.act(this);
        }

        @Override
        public void complete() {
            // TODO Auto-generated method stub
        }
    }

    static class Add extends Action<Record> {
        private long load;

        public Add(long load) {
            this.load = load;
        }

        @Override
        public void act(Record record) {
            record.counter+=load;
        }
    }

    static class Get extends Action<Record> {
        PortFuture<Long> sink;
        
        public Get(PortFuture<Long> sink) {
            this.sink = sink;
        }
        
        @Override
        public void act(Record ship) {
            sink.send(ship.counter);
        }
    }

    static class Words extends AbstractDemux<String, Action<Record>, Record> {

        @Override
        protected AbstractDelegator<Action<Record>, Record> createDelegator(String tag) {
            return new LiberalDelegator<Record>();
        }

        @Override
        protected void requestHandler(String tag, AbstractDelegator<Action<Record>, Record> gate) {
            gate.handler.send(new Record());
        }
    }


    @Test
    public void test() throws InterruptedException {
        Words numbers=new Words();
        PortFuture<Long> sink1 = new PortFuture<Long>();
        PortFuture<Long> sink2 = new PortFuture<Long>();
        numbers.send(1L, new Add(1));
        numbers.send(2L, new Add(1));
        numbers.send(1L, new Add(-11));
        numbers.send(1L, new Get(sink1));
        numbers.send(2L, new Add(11));
        numbers.send(2L, new Get(sink2));
        assertEquals(Long.valueOf(-10L), sink1.get());
        assertEquals(Long.valueOf(12L), sink2.get());
    }

    public static void main(String[] args) throws InterruptedException {
        WordCountTest smokeTest = new WordCountTest();
        smokeTest.test();
    }
}

