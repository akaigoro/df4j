package com.github.rfqu.df4j.nio2.echo2;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.nio2.IOCallback;
import com.github.rfqu.df4j.nio2.IORequest;

public abstract class IOHandler<T extends IORequest<T>>
  extends ActorVariable<T>
  implements IOCallback<T>
{
    @Override
    public final void send(T request) {
        request.toCallback(this);
    }

    @Override
    protected void act(T request) throws Exception {
        request.toCallback(this);
    }

    @Override
	public void timedOut(T request) {
	}

	@Override
	public void closed(T request) {
	}

	@Override
	public void failed(Throwable exc, T request) {
	}

}