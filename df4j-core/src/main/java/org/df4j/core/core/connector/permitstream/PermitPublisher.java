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

package org.df4j.core.core.connector.permitstream;

/**
 * A {@link PermitPublisher} is a provider of a potentially unbounded number of permits
 * <p>
 */
public interface PermitPublisher {

    /**
     * was: onSendTo
     *
     * @param subscriber
     *      the {@link PermitSubscriber} that will consume signals from this {@link PermitPublisher}
     */
    public void subscribe(PermitSubscriber subscriber);
}
