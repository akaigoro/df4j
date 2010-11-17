package dffw;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author rfq
 *
 * @param <R> type of result
 */
public abstract class Node<R> implements Runnable {
    protected DFExecutor executor;
    ArrayList<Port<?>> ports=new ArrayList<Port<?>>();
    ArrayList<Node<?>.Port<R>> sinks=new ArrayList<Node<?>.Port<R>>();
    boolean done=false;
    R result;
    Exception exception;

    public Node(DFExecutor e) {
        this.executor=e;
    }
    
    /** connects this node (as a source) to a port (as a sink)
     * @param sink
     * @throws ExecutionException 
     */
    public synchronized void connect(Node<?>.Port<R> sink) throws ExecutionException {
        if (done) {
            sink.put(result);
        } else {
            sinks.add(sink);
        }
    }

    private void checkFire() {
        for (Port<?> p: ports) {
            if (!p.filled) return;
        }
        // submit task if data driven
        // for demand - driven, check sinks
        // if (sinks.size()==0) return;
//        res = (FutureTask<R>) executor.submit(this);
        executor.execute(this);
    }
    
    public synchronized R get() throws InterruptedException, ExecutionException {
        while (!done) {
            wait();
        }
        return result;
    }

    abstract R call() throws Exception;
    
//    @Override
    public void run() {
        Exception exc=null;
        R res=null;
        try {
            Thread.sleep(10000);
            res=call();
        } catch (Exception e) {
            exc=e;
        }
        setResult(res, exc);
        System.out.println("res="+result+"; exception="+exception);
    }

    synchronized void setResult(R res, Exception exc) {
        result=res;
        exception=exc;
        done=true;
        for (Node<?>.Port<R> sink: sinks) {
            sink.put(res); // TODO exception handling
        }
        sinks.clear();
        notifyAll();
    }

    public class Port<V> {
        boolean filled=false;
        V value;
        
        public Port() {
           ports.add(this);
        }
        public void put(V value) {
            this.value=value;
            filled=true;
            checkFire();
        }
        public V get() {
            return value;
        }
        void connect(Node<V> node) throws ExecutionException {
            node.connect(this);
        }
    }
}
