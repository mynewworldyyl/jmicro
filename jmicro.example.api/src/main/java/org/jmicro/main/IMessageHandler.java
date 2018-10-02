package org.jmicro.main;

import org.jmicro.api.server.Message;

public interface IMessageHandler {

	public void onMessage(Message msg);
}
