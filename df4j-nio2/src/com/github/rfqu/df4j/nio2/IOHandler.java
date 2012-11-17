package com.github.rfqu.df4j.nio2;

import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.InterruptedByTimeoutException;

import com.github.rfqu.df4j.core.Port;

public abstract class IOHandler<T extends IORequest<T>>
  implements Port<T>, IOCallback<T>
{

    @Override
    public final void send(T request) {
    	final Throwable exc = request.getExc();
		if (exc == null) {
            final Integer result = request.getResult();
			if (result==-1) {
            	closed(request);
            } else {
            	completed(result, request);
            }
        } else {
            if (exc instanceof AsynchronousCloseException) {
                // System.out.println("  ServerRequest conn closed id="+id);
            	closed(request);
            } else if (exc instanceof InterruptedByTimeoutException) {
            	timedOut(request);
            } else {
                // System.out.println("  ServerRequest read failed id="+id+"; exc="+exc);
            	failed(exc, request);
            }
        }
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