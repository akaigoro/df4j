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

package org.df4j.core.spi.permitstream;

/**
 * A {@link PermitProvider} is a provider of a potentially unbounded number of permits
 * <p>
 */
public interface PermitProvider {

    /**
     * was: onSendTo
     *
     * @param consumer
     *      the {@link PermitConsumer} that will consume signals from this {@link PermitProvider}
     */
    public void connect(PermitConsumer consumer);
}
