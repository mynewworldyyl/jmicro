package org.jmicro.common.channel;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

import org.jmicro.common.channel.ObjectSelector.Elt;

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
