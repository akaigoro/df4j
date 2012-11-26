package com.github.rfqu.df4j.nio;

public interface IOCallback<T extends IORequest<T>> {
	public void completed(int result, T request);// throws Exception;

	public void timedOut(T request) ;

	public void closed(T request);

	public void failed(Throwable exc, T request);
}
