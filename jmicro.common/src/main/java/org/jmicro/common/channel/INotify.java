package org.jmicro.common.channel;

public interface INotify<T> {

	void notify(ObjectChannel<T> channel,int opts);
}
