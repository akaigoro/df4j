package com.github.rfqu.df4j.ioexample.dock;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import com.github.rfqu.df4j.core.Port;

public class PageCache {
    PagedFile pf;
    int numPages;
    protected ArrayBlockingQueue<Page> freePages;
    protected ArrayBlockingQueue<Port<Page>> pageRequests;
    HashMap<Integer, Dock> cache=new HashMap<Integer, Dock>();
    
    public PageCache(PagedFile pf, int numPages) {
        this.pf=pf;
        this.numPages=numPages;
        freePages=new ArrayBlockingQueue<Page>(numPages);
        pageRequests=new ArrayBlockingQueue<Port<Page>>(numPages);
    }

    public void send(Integer key, PageUser message) {
        Dock dock;
        synchronized (this) {
            dock=cache.get(key);
            if (dock==null) {
                dock=new Dock(pf);
                cache.put(key, dock);
                Page newPage=freePages.poll();
                if (newPage==null) {
                    pageRequests.add(dock.pagePort);
                } else {
                    dock.pagePort.send(newPage);
                }
            }
        }
        dock.send(message);
    }
}
