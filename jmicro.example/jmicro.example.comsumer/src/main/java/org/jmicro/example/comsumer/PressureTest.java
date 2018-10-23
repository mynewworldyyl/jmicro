package org.jmicro.example.comsumer;

import java.util.Random;

import org.jmicro.api.JMicro;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;

public class PressureTest {

	public static void main(String[] args) {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		for(int i = 0; i < 1;i++){
			new Thread(new Worker(of)).start();
		}
	}
}

class Worker implements Runnable{

	private IObjectFactory of;
	private Random r = new Random();
	
	public Worker(IObjectFactory of){
		this.of = of;
	}
	
	@Override
	public void run() {
		TestRpcClient src = of.get(TestRpcClient.class);
		
		for(;;){
			//invoke remote service
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			src.invokePersonService();
		}
	}
	
}
