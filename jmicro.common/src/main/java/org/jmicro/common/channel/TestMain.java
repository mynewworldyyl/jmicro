package org.jmicro.common.channel;

import java.io.IOException;
import java.nio.channels.Selector;

public class TestMain {

	public static void main(String[] args) {
		
		new Thread(()->{
			try {
				Selector selector = ObjectChannel.provider.openSelector();
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}).start();
		
	}
	
}
