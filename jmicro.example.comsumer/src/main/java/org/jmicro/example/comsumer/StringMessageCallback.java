package org.jmicro.example.comsumer;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.client.IMessageCallback;

@Component(value="stringMessageCallback")
public class StringMessageCallback implements IMessageCallback<String> {

	@Override
	@JMethod
	public void onMessage(String msg) {
		System.out.println("Client got message: "+ msg);
	}
	
}
