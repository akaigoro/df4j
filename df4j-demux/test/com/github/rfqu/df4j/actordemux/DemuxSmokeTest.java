package com.github.rfqu.df4j.actordemux;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.CallbackFuture;

public class DemuxSmokeTest {

    static class Record {
        long val;
    }

    static class Add extends Action<Record> {
        private long load;

        public Add(long load) {
            this.load = load;
        }

        @Override
        public void act(Record record) {
            record.val+=load;
        }
    }

    static class Get extends Action<Record> {
        CallbackFuture<Long> sink;
        
        public Get(CallbackFuture<Long> sink) {
            this.sink = sink;
        }
        
        @Override
        public void act(Record ship) {
            sink.send(ship.val);
        }
    }

    static class Numbers extends LiberalDemux<Long, Action<Record>, Record> {

        @Override
        protected void requestHandler(Long tag, Port<Record> handler) {
            handler.send(new Record());
        }
    }

    @Test
    public void test() throws InterruptedException {
        Numbers numbers=new Numbers();
        CallbackFuture<Long> sink1 = new CallbackFuture<Long>();
        CallbackFuture<Long> sink2 = new CallbackFuture<Long>();
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
        DemuxSmokeTest smokeTest = new DemuxSmokeTest();
        smokeTest.test();
    }
}

