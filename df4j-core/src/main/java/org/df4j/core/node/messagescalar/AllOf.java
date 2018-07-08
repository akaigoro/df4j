package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarSubscriber;

public class AllOf<T> extends AsyncResultFuture<T[]> {
    T[] results;

    public AllOf() {
    }

    public AllOf(AsyncResult<T>... sources) {
        results = (T[]) new Object[sources.length];
        for (int k = 0; k<sources.length; k++) {
            AsyncResult source = sources[k];
            final Enter arg = new Enter(k);
            source.subscribe(arg);
        }
    }

    @Override
    protected void fire() {
        complete(results);
    }

    class Enter extends Lock implements ScalarSubscriber<T> {
        private final int num;

        public Enter(int num) {
            this.num = num;
        }

        @Override
        public void post(T value) {
            results[num] = value;
            super.turnOn();
        }

        @Override
        public void postFailure(Throwable ex) {
            synchronized (AllOf.this) {
                if (!result.isDone()) {
                    AllOf.this.completeExceptionally(ex);
                }
            }
        }
    }

}
