package org.df4j.core.activities;

import org.df4j.core.actor.AbstractProcessor;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

/*

 */
public class ProcessorActor extends AbstractProcessor<Long,Long> {
    Logger logger = LoggerFactory.getLogger("processor");
    final int delay;

    public ProcessorActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected Long whenNext(Long item) {
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
        return item;
    }

    @Override
    public void whenComplete(Throwable th) {
        logger.info("  got: completed with "+th);
    }
}
