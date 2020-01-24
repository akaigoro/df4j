package org.df4j.protocol;

/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/
/**
 * Flow of messages with back-pressure
 * <p>
 *  This class declaration is for reference only.
 *  Actual declarations used are in package {@link org.reactivestreams}
 * @see <a href="https://www.reactive-streams.org/">https://www.reactive-streams.org/</a>
 */
public final class Flow {

    private Flow() {} // uninstantiable

    /**
     * A {@link Subscription} represents a one-to-one lifecycle of a {@link Subscriber} subscribing to a {@link Publisher}.
     * <p>
     * It can only be used once by a single {@link Subscriber}.
     * <p>
     * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
     *
     */
    public interface Subscription extends org.reactivestreams.Subscription, SimpleSubscription {
    }
}
