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

package org.df4j.core.boundconnector.permitstream;

import org.df4j.core.boundconnector.SimpleSubscription;

/**
 * A {@link PermitPublisher} is a provider of a potentially unbounded number of permits
 */
@FunctionalInterface
public interface PermitPublisher {

    /**
     * was: onSendTo
     *
     * @param subscriber
     *      the {@link PermitSubscriber} that will consume signals from this {@link PermitPublisher}
     * @return SimpleSubscription which can be cancelled, or null if subscribtion object is not created
     */
    SimpleSubscription subscribe(PermitSubscriber subscriber);
}
