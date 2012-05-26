package com.github.rfqu.df4j.actordemux;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

class Record implements Delegate<Action<Record>>{
    long val;

	@Override
	public void act(Action<Record> message) {
		message.act(this);
	}

	@Override
	public void complete() {
		// TODO Auto-generated method stub
		
	}
}

class Add extends Action<Record> {
    private long load;

    public Add(long load) {
        this.load = load;
    }

    @Override
	public void act(Record record) {
        record.val+=load;
    }
}

class Print extends Action<Record> {
    CountDownLatch count;
    
    public Print(CountDownLatch count) {
        this.count = count;
    }
    
    @Override
	public void act(Record ship) {
        System.out.println("load="+ship.val);
        count.countDown();
    }
}

class Numbers extends AbstractDemux<Long, Action<Record>, Record> {

	@Override
	protected AbstractDelegator<Action<Record>, Record> createDelegator(Long tag) {
		return new LiberalDelegator<Record>();
	}

	@Override
	protected void requestHandler(Long tag, AbstractDelegator<Action<Record>, Record> gate) {
        gate.handler.send(new Record());
	}
}

public class DemuxSmokeTest {

    @Test
    public void test() throws InterruptedException {
        Numbers numbers=new Numbers();
        CountDownLatch count=new CountDownLatch(2);
        numbers.send(1L, new Add(1));
        numbers.send(2L, new Add(1));
        numbers.send(1L, new Add(-11));
        numbers.send(1L, new Print(count));
        numbers.send(2L, new Add(11));
        numbers.send(2L, new Print(count));
        count.await();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DemuxSmokeTest smokeTest = new DemuxSmokeTest();
        smokeTest.test();
    }
}

