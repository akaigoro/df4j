package com.github.rfqu.df4j.nio2.echo;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio.IOCallback;
import com.github.rfqu.df4j.nio.IORequest;

public abstract class IOHandler<T extends IORequest<T>>
  implements Port<T>, IOCallback<T>
{
    @Override
    public final void send(T request) {
        request.toIOCallback(this);
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