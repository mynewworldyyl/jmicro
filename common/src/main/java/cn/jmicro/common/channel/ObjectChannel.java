/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.common.channel;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:32
 */
public class ObjectChannel<T> extends AbstractSelectableChannel implements IWritable<T>,IReadable<T>{

	private int maxEltNum = 100000;
	
	private int opts = 0;
	
	private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>(); 
	
	private INotify<T> notifier = null;
	
	public static SelectorProvider provider = new SelectorProvider(){

		@Override
		public DatagramChannel openDatagramChannel() throws IOException {
			 throw new IOException("not support openDatagramChannel");
		}

		@Override
		public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
			throw new IOException("not support openDatagramChannel");
		}

		@Override
		public Pipe openPipe() throws IOException {
			throw new IOException("not support openPipe");
		}

		@Override
		public AbstractSelector openSelector() throws IOException {
			return new ObjectSelector(this);
		}

		@Override
		public ServerSocketChannel openServerSocketChannel() throws IOException {
			throw new IOException("not support openServerSocketChannel");
		}

		@Override
		public SocketChannel openSocketChannel() throws IOException {
			throw new IOException("not support openSocketChannel");
		}
		
	};
	

	public ObjectChannel() {
		super(provider);
	}
	
	@Override
	protected void implCloseSelectableChannel() throws IOException {
		synchronized(blockingLock()) {
			queue.clear();
		}
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		
	}

	@Override
	public int validOps() {
		return opts;
	}

	@Override
	public T read()  throws IOException{
		if(queue.isEmpty() && this.isBlocking()) {
			synchronized(blockingLock()){
				try {
					blockingLock().wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
		}
		T elt = queue.poll();
		blockingLock().notify();
		this.notifier.notify(this, SelectionKey.OP_WRITE);
		return elt;
	}

	@Override
	public void write(T o) throws IOException{
		if(queue.size() > this.maxEltNum && this.isBlocking()) {
			synchronized(blockingLock()){
				try {
					blockingLock().wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
		}
		queue.offer(o);
		blockingLock().notify();
		this.notifier.notify(this, SelectionKey.OP_READ);
	}

	public void setNotifier(INotify notifier) {
		this.notifier = notifier;
	}
		
}
