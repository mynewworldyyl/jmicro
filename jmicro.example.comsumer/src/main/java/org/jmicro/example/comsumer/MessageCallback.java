package org.jmicro.example.comsumer;

import org.jmicro.api.annotation.Component;

@Component
public class MessageCallback {

	
	public void onMessage(String msg){
		System.out.println("Got message: "+ msg);
	}
}
