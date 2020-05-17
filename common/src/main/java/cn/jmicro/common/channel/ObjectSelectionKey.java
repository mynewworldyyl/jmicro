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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

import cn.jmicro.common.channel.ObjectSelector.Elt;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:41
 */
public class ObjectSelectionKey extends AbstractSelectionKey {

	private int readyOpts = 0;
	
	private Elt elt = null;
	
	private ObjectSelector selector;
	
	public ObjectSelectionKey(ObjectSelector selector) {
		this.selector = selector;
	}
	
	public void setElt(Elt elt) {
		this.elt = elt;
	}

	@Override
	public SelectableChannel channel() {
		return this.elt.ch;
	}

	@Override
	public Selector selector() {
		return this.selector();
	}

	@Override
	public int interestOps() {
		return this.elt.ops;
	}

	@Override
	public SelectionKey interestOps(int ops) {
		this.elt.ops = ops;
		return this;
	}

	@Override
	public int readyOps() {
		// TODO Auto-generated method stub
		return this.readyOpts;
	}

	public void readyOps(int readyOpts) {
		this.readyOpts = readyOpts;
	}
}
