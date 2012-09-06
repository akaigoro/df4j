package com.github.rfqu.df4j.actordemux;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;


public class WordCountTest {

    static class Counter extends Link {
        long counter=0;
        String tag;
        public Counter(String tag) {
            this.tag=tag;
        }
    }

    static class Words extends LiberalDemux<String, Counter> {

        @Override
        protected void requestHandler(String tag, Port<Counter> handler) {
            handler.send(new Counter(tag));
        }
    }

    static class Add extends Action<String, Counter> {
        @Override
        public void act(String tag, Counter record) {
            record.counter++;
        }
    }

    static class Getter extends Action<Counter> {
        Dictionary sink;
        
        public Getter(Dictionary dictionary) {
            this.sink = dictionary;
        }
        
        @Override
        public void act(Counter counter) {
            sink.send(counter.tag, counter);
        }
    }

    static class Entry implements Delegate<Counter>{
        String tag;
        long counter=0;
        
        public Entry(String tag) {
            this.tag=tag;
        }
        @Override
        public void act(Counter message) {
            counter+=message.counter;
        }
        
        @Override
        public void complete() {
            // TODO Auto-generated method stub
            
        }
    }

    static class Dictionary extends ConservativeDemux<String, Counter, Entry> {

        @Override
        protected void requestHandler(String tag, Port<Entry> handler) {
            handler.send(new Entry(tag));
        }
    }

    @Test
    public void test() throws InterruptedException {
        Words words=new Words();
        CallbackFuture<Long> sink1 = new CallbackFuture<Long>();
        CallbackFuture<Long> sink2 = new CallbackFuture<Long>();
        words.send("aa", new Add());
        words.send("bb", new Add());
        words.send("c", new Add());
        words.send("aa", new Add());
        Dictionary dictionary=new Dictionary();
        Getter getter=new Getter(dictionary);
        words.sendAll(getter);
        assertEquals(Long.valueOf(12L), sink2.get());
    }

    public static void main(String[] args) throws InterruptedException {
        WordCountTest smokeTest = new WordCountTest();
        smokeTest.test();
    }
}

