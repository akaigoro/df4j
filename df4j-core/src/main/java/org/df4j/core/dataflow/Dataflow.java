package org.df4j.core.dataflow;

import org.df4j.core.util.linked.LinkedQueue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

/**
 * A dataflow graph, consisting of 1 or more {@link AsyncProc}s and, probably, nested {@link Dataflow}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link AsyncProc}s plays the same role as basic blocks in a flow chart.
 */
public class Dataflow extends Node<Dataflow> implements Activity {
    protected ExecutorService executor;
    protected Timer timer;
    protected LinkedQueue<Node> children = new LinkedQueue<>();

    /**
     *  creates root {@link Dataflow} graph.
     */
    public Dataflow() {
    }

    @Override
    public Dataflow getItem() {
        return this;
    }

    /**
     *  creates nested {@link Dataflow} graph.
     * @param parent the parent {@link Dataflow}
     */
    public Dataflow(Dataflow parent) {
        this.parent = parent;
        parent.enter(this);
    }

    public void setExecutor(ExecutorService executor) {
        bblock.lock();
        try {
            this.executor = executor;
        } finally {
            bblock.unlock();
        }
    }

    public void setExecutor(Executor executor) {
        ExecutorService service = new AbstractExecutorService(){
            @Override
            public void execute(@NotNull Runnable command) {
                executor.execute(command);
            }

            @Override
            public void shutdown() {

            }

            @NotNull
            @Override
            public List<Runnable> shutdownNow() {
                return null;
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
                return false;
            }
        };
        setExecutor(service);
    }

    public ExecutorService getExecutor() {
        bblock.lock();
        try {
            if (executor == null) {
                if (parent != null) {
                    executor = parent.getExecutor();
                } else {
                    Thread currentThread = Thread.currentThread();
                    if (currentThread instanceof ForkJoinWorkerThread) {
                        executor = ((ForkJoinWorkerThread) currentThread).getPool();
                    } else {
                        executor = ForkJoinPool.commonPool();
                    }
                }
            }
            return executor;
        } finally {
            bblock.unlock();
        }
    }

    public void setTimer(Timer timer) {
        bblock.lock();
        try {
            this.timer = timer;
        } finally {
            bblock.unlock();
        }
    }

    public Timer getTimer() {
        bblock.lock();
        try {
            if (timer != null) {
                return timer;
            } else if (parent != null) {
                return timer = parent.getTimer();
            } else {
                return timer = getSingletonTimer();
            }
        } finally {
            bblock.unlock();
        }
    }

    /**
     * indicates that a node has added to this graph.
     * @param node
     */
    public void enter(Node node) {
        bblock.lock();
        try {
            children.add(node);
        } finally {
            bblock.unlock();
        }
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     * @param node
     */
    public void leave(Node node) {
        bblock.lock();
        try {
            children.remove(node);
            if (children.size() == 0) {
                super.onComplete();
                if (parent != null) {
                    parent.leave(this);
                }
            }
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive() {
        return !super.isCompleted();
    }

    public void onError(Throwable t) {
        super.onError(t);
        if (parent != null) {
            parent.onError(t);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isCompleted()) {
            sb.append("not completed");
        } else if (this.completionException == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(this.completionException.toString());
        }
        sb.append("; child node count: "+children.size());
        return sb.toString();
    }

    private static Timer singletonTimer;

    @NotNull
    public static Timer getSingletonTimer() {
        Timer res = singletonTimer;
        if (res == null) {
            synchronized (Dataflow.class) {
                res = singletonTimer;
                if (res == null) {
                    res = singletonTimer = new Timer();
                }
            }
        }
        return res;
    }

}
