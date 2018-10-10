package org.jmicro.api.client;

import org.jmicro.api.server.Message;

public interface IClientMessageHandler {
	public Short type();
	public void onResponse(IClientSession session,Message msg);
}
