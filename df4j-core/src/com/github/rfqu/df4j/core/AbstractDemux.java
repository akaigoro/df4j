package com.github.rfqu.df4j.core;

import java.util.concurrent.ConcurrentHashMap;


/** Associative array of decoupled actors. Yes, this is tagged dataflow.
 * 
 * @author rfq
 *
 * @param <Tag>
 * @param <H>
 */
public abstract class AbstractDemux<Tag, M extends Link, H>
	implements TaggedPort <Tag, M>
{
    protected ConcurrentHashMap<Tag, AbstractDelegator<M, H>> cache
    	= new ConcurrentHashMap<Tag, AbstractDelegator<M, H>>();

    @Override
    public void send(Tag tag, M message) {
        boolean dorequest=false;
        AbstractDelegator<M, H> gate=cache.get(tag);
        if (gate==null) {
            synchronized (this) {
                gate=cache.get(tag);
                if (gate==null) {
                    gate=createDelegator(tag);
                    cache.put(tag, gate);
                    dorequest=true;
                }
            }
        }
        if (dorequest) {
            requestHandler(tag, gate);
        }
        gate.send(message);
    }

    protected abstract AbstractDelegator<M,H> createDelegator(Tag tag);
    protected abstract void requestHandler(Tag tag, AbstractDelegator<M,H> gate);
}
