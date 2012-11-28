/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.DataflowNode.Semafor;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 */
public class AsyncServerSocketChannel extends ActorVariable<Callback<SocketChannel>>
	implements ServerSocketEventListener
{
  private final Semafor pending=new Semafor();

  private SocketAddress addr;
  private ServerSocketChannel channel;
  private SelectorThread currentSelectorThread ;
  protected volatile boolean interestOn=false;
  private Promise<SocketAddress> closeEvent=new Promise<SocketAddress>();
  
  public AsyncServerSocketChannel(SocketAddress addr) throws IOException {
	  this.addr=addr;
      channel = ServerSocketChannel.open();
      channel.configureBlocking(false);
      channel.socket().bind(addr);
      currentSelectorThread=SelectorThread.getCurrentSelectorThread();
      currentSelectorThread.register(channel, SelectionKey.OP_ACCEPT, this);
      interestOn=true;
      pending.up(); // allow accept
  }
  
  /** initiates acceptance process when the channel is free
   * @param acceptor
   */
	public void accept(Callback<SocketChannel> acceptor) {
		input.send(acceptor);
	}
	
  public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
  	closeEvent.addListener(listener);
      return listener;
  }
  
  public ServerSocketChannel getChannel() {
      return channel;
  }

	//======================== Dataflow backend

  @Override
	protected void act(Callback<SocketChannel> acceptor) {
      try {
          SocketChannel sch = channel.accept();
          if (sch==null) {
          	pushback();
          	return;
          }
          acceptor.send(sch);
      } catch (IOException exc) {
          acceptor.sendFailure(exc);
      }
      // pending.down() made automatically by Pin's logic
	}

	@Override
	protected void complete() throws Exception {
      try {

			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
      channel = null;
		closeEvent.send(addr);
	}

	@Override
	protected void handleException(Throwable exc) {
		close();  // eventually leads to complete() => channel.close();
      for (Callback<?> acceptor: super.input) {
      	acceptor.sendFailure(exc);
      }
  }
  
    //========================= ServerSocketEventListener backend

    /** new client wants to connect */
    @Override
    public void accept(SelectionKey key) {
    	pending.up();
    }

}
