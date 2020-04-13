package org.jmicro.api.timer;

public interface ITickerAction<T> {
	void act(String key,T attachement);
}
