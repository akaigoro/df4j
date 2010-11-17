package dffw;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DFExecutor extends ThreadPoolExecutor {
    
    public DFExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>());
    }
    
    public DFExecutor() {
        this(1, Runtime.getRuntime().availableProcessors(), 5, TimeUnit.SECONDS);
    }
    
    protected void afterExecute(Runnable r, Throwable t) {
        System.out.println("afterExecute:"+r);
    }
}