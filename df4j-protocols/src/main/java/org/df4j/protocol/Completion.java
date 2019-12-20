package org.df4j.protocol;

/**
 * A {@link Signal} with completion exceptions
 * Borrowed from RxJava project
 */
public class Completion {
    private Completion() {}

    /**
     * consumable via an {@link CompletableObserver}.
     */
    public interface CompletableSource<S extends CompletableObserver> {

        boolean isCompleted();

        /**
         * Subscribes the given CompletableObserver to this CompletableSource instance.
         * @param co the CompletableObserver, not null
         * @throws NullPointerException if {@code co} is null
         */
        void subscribe(S co);

        boolean unsubscribe(S co);
    }

    /**
     * Provides a mechanism for receiving push-based notification of a valueless completion or an error.
     * <p>
     * A well-behaved {@code CompletableSource} will call a {@code CompletableObserver}'s {@link #onError(Throwable)}
     * or {@link #onComplete()} method exactly once as they are considered mutually exclusive <strong>terminal signals</strong>.
     * <p>
     * Calling the {@code CompletableObserver}'s method must happen in a serialized fashion, that is, they must not
     * be invoked concurrently by multiple threads in an overlapping fashion and the invocation pattern must
     * adhere to the following protocol:
     * <pre><code>    onSubscribe (onError | onComplete)?</code></pre>
     * <p>
     * Subscribing a {@code CompletableObserver} to multiple {@code CompletableSource}s is not recommended. If such reuse
     * happens, it is the duty of the {@code CompletableObserver} implementation to be ready to receive multiple calls to
     * its methods and ensure proper concurrent behavior of its business logic.
     * <p>
     * The implementations of the {@code onXXX} methods should avoid throwing runtime exceptions other than the following cases:
     * <ul>
     * <li>If the argument is {@code null}, the methods can throw a {@code NullPointerException}.
     * Note though that RxJava prevents {@code null}s to enter into the flow and thus there is generally no
     * need to check for nulls in flows assembled from standard sources and intermediate operators.
     * </li>
     * <li>If there is a fatal error (such as {@code VirtualMachineError}).</li>
     * </ul>
     */
    public interface CompletableObserver extends Signal.Subscriber {
        /**
         * Called once if the deferred computation 'throws' an exception.
         * @param e the exception, not null.
         */
        void onError(Throwable e);
        
        /**
         * Called once the deferred computation completes normally.
         */
        default void onComplete() {
            onError(null);
        }
    }

}
