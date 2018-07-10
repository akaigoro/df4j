package org.df4j.core.node.messagescalar;

import org.df4j.core.node.Action;

import java.util.function.Supplier;

/**
 * a node with an output parameter
 *
 * @param <R>
 */
public class AsyncSupplier<R> extends AsyncResult<R> {
    @Action
    protected Supplier<? extends R> proc;

    public AsyncSupplier() {
    }

    public AsyncSupplier(Supplier<? extends R> proc) {
        this.proc = proc;
    }

    public AsyncSupplier(Runnable runnable) {
        this.proc = ()->{runnable.run(); return null;};
    }
}
