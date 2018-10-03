package org.jmicro.example.api;

import org.jmicro.api.server.Message;

public interface IMessageHandler {

	public void onMessage(Message msg);
}
