package org.jmicro.api.client;

public interface IMessageCallback<T> {

	void onMessage(T msg);
}
