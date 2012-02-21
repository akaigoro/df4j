package com.github.rfqu.df4j.ioexample.dock;

import java.util.concurrent.ArrayBlockingQueue;

import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Task;

public class Dock extends Task {//implements Port<PageUser> {
    PagedFile pf;
    protected final boolean eager;
    PagePool ppool;
    int key;
    MessageQueue<Action> actions=new MessageQueue<Action>();
    Page page;
    boolean running;
    
    public Dock(PagePool ppool, int key) {
        this.ppool=ppool;
        this.key=key;
    }
    public Dock(PagedFile pf) {
        this.pf=pf;
        this.eager=false;
    }
    
    /**
     * @param eager if false, the actor would process messages immediate a
     * at the invocation of 'send', if no other messages are pending.
     */
    public Dock(boolean eager) {
        this.eager=eager;
    }

    
    public void setPage(Page page) {
        page.init(key);
        this.page=page;
        if (actions.isEmpty()) {
            return;
        }
        fire();
    }
    
    public Page removePage() {
        if (!actions.isEmpty()) {
            throw new IllegalStateException("page is not free");
        }
        if (page==null) {
            throw new IllegalStateException("page is null");
        }
        Page res = page;
        page=null;
        return res;
    }
    
    public void send(Action action) {
        if (action==null) {
            throw new NullPointerException();
        }
        if (actions.isEmpty()) {
            ppool.getBack(this); // from ppool.idleDocks
            if (page==null) {
                ppool.aquirePage(this);
            }
        }
        actions.add(action);
        if (running || (page==null)) {
            return;
        }
        fire();
    }

    public Task fire() {
        running=true;
        return super.fire();
    }

    @Override
    public void run() {
        for (;;) {
            Action message;
            synchronized (this) {
                message = actions.poll();
                if ((message == null) ) {
                    running = false;
                    ppool.putIdle(this);
                    return;
                }
            }
            try {
                message.act(page);
            } catch (Exception e) {
            }
        }
    }


    static final int queueSize=10;
    protected ArrayBlockingQueue<PageUser> input=new ArrayBlockingQueue<PageUser>(queueSize);
    public Port<Page> pagePort=new Port<Page>() {
        @Override
        public void send(Page newPage) {
            if (page!=null) {
                throw new IllegalStateException("another Page");
            }
            synchronized(this) {
                page=newPage;
                if (!input.isEmpty()) {
                    running=true;
                }
            }
            fire();
        }      
    };

    @Override
    public void send(PageUser message) {
        check(message);
        synchronized(this) {
//          inpCount++;
          if (running || (page==null)) {
             input.add(message);
             return;
          }
          // ready but not running
          if (!eager) {
              input.add(message);
          }
          running=true;
      }
      if (eager) {
          doAct(message);
          synchronized(this) {
              if (input.isEmpty()) {
                  running=false;
                  return;
              }
          }
      }
      fire();
    }

    /** loops through the accumulated message queue
     */
    @Override
    public void run() {
        for (;;) {
            PageUser message;
            synchronized (this) {
                message = input.poll();
                if ((message == null)) {
                    running = false;
                    return;
                }
            }
            doAct(message);
        }
    }

    void doAct(PageUser message) {
        try {
            message.usePage(page);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void check(PageUser user) {
        if (user==null) {
            throw new NullPointerException("message may not be null");
        }
    }
}
