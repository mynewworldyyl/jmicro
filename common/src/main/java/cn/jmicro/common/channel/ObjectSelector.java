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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:49
 */
public class ObjectSelector<T> extends AbstractSelector implements INotify<T>{

	private static final int MAX_CHANNELS = 10;
	
	private Elt<T>[] ochannels = null;
	
	private Set<SelectionKey> selectedKeys = null;
	
	private int index = -1;
	
	private Object locker = new Object();
	
	public static class Elt<T> {
		public ObjectChannel<T> ch;
		public int ops;
		public Object att;
		public ObjectSelectionKey key;
		
		public Elt(ObjectChannel<T> ch, int ops, Object att, ObjectSelectionKey key) {
			this.ch = ch;
			this.ops = ops;
			this.att = att;
			this.key = key;
		}
	}
	
	public ObjectSelector(SelectorProvider provider) {
		super(provider);
		this.ochannels = new Elt[MAX_CHANNELS];
		this.selectedKeys = new HashSet<>(MAX_CHANNELS);
	}
	
	@Override
	public void notify(ObjectChannel<T> channel,int opts) {
		Elt<T> e = getElt(channel);
		
		if(e == null) {
			return;
		}
		
		if( (e.ops & opts) ==0 ) {
			return;
		}
		e.key.readyOps(opts);
		selectedKeys.add(e.key);
		this.wakeup();
	}

	private Elt<T> getElt(ObjectChannel<T> channel) {
		for(int i = 0; i < this.index; i ++) {
			if(channel == this.ochannels[i].ch) {
				return this.ochannels[i];
			}
		}
		return null;
	}

	@Override
	protected void implCloseSelector() throws IOException {
			
	}

	@Override
	protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
		if(ochannels.length > MAX_CHANNELS) {
			throw new IndexOutOfBoundsException("Max channel support is: " + MAX_CHANNELS);
		}
		ObjectChannel<T> oc = (ObjectChannel<T>)ch;
		oc.setNotifier(this);
		ObjectSelectionKey key = new ObjectSelectionKey(this);
		Elt e = new Elt(oc,ops,att,key);
		key.setElt(e);
		ochannels[++this.index] = e;
		return key;
	}

	@Override
	public Set<SelectionKey> keys() {
		Set<SelectionKey> ks = new HashSet<SelectionKey>();
		for(int i = 0; i < this.index; i ++) {
			ks.add(this.ochannels[i].key);
		}
		return ks;
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return this.selectedKeys;
	}

	@Override
	public int selectNow() throws IOException {
		if(this.selectedKeys.isEmpty()) {
			synchronized(locker) {
				try {
					locker.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return this.selectedKeys.size();
	}

	@Override
	public int select(long timeout) throws IOException {
		if(!this.selectedKeys.isEmpty()) {
			return this.selectedKeys.size();
		}else {
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				@Override
				public void run() {
					ObjectSelector.this.locker.notify();
				}
			}, timeout);
			
			synchronized(locker) {
				try {
					locker.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return this.selectedKeys.size();
	}

	@Override
	public int select() throws IOException {
		return selectNow();
	}

	@Override
	public Selector wakeup() {
		this.locker.notify();
		return this;
	}

}
