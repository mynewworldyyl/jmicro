package org.jmicro.example.api;

import org.jmicro.api.net.Message;

public interface IMessageHandler {

	public void onMessage(Message msg);
}
