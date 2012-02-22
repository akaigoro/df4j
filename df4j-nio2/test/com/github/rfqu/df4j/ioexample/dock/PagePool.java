package com.github.rfqu.df4j.ioexample.dock;

import java.util.concurrent.CountDownLatch;

import com.github.rfqu.df4j.core.LinkedQueue;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;


public class PagePool {

	final int size;
	LinkedQueue<Dock> idleDocks=new LinkedQueue<Dock>();
	LinkedQueue<Dock> dock2Load=new LinkedQueue<Dock>();
	int pageCount=0;
	
	public PagePool(int size) {
		this.size=size;
	}
	
	public void aquirePage(Dock hdock) {
		if (pageCount<size) {
			pageCount++;
			hdock.setPage(new Page());
			return;
		}
		Dock iDock=idleDocks.poll();
		if (iDock!=null) {
			hdock.setPage(iDock.removePage());
			return;
		}
		dock2Load.add(hdock);
	}
	
	public void putIdle(Dock iDock) {
		Dock hDock=dock2Load.poll();
		if (hDock!=null) {
			hDock.setPage(iDock.removePage());
			return;
		}
		idleDocks.add(iDock);
	}
	
	public void getBack(Dock dock) {
		dock.unlink();
	}
	
	/* basic test
	 */
	public static void main(String[] args) throws InterruptedException {
		SimpleExecutorService executor = new SimpleExecutorService();
		Task.setCurrentExecutor(executor);
		PagePool pp=new PagePool(2);
		Dock[] docks=new Dock[5];
		for (int key=0; key<docks.length; key++) {
			docks[key]=new Dock(pp,key);
		}
		for (int i = 0; i < 2; i++) {
			final CountDownLatch sink=new CountDownLatch(11);
			for (int k = 0; k < sink.getCount(); k++) {
				final int kk = k;
				final int key = k % docks.length;
				Dock d = docks[key];
				d.send(new Action() {
					void act(Page p) {
						System.out.println("k=" + kk + "; key=" + key+ "; p.key=" + p.key);
						sink.countDown();
					}
				});
			}
			sink.await();
		}
	}
}
