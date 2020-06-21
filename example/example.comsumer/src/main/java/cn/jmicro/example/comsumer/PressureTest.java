package cn.jmicro.example.comsumer;

import java.util.Random;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.example.api.rpc.ISimpleRpc;

public class PressureTest {

	public static void main(String[] args) {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=PressureTest"});
		for(int i = 0; i < 1;i++){
			new Thread(new Worker(of,i)).start();
		}
	}
}

class Worker implements Runnable{

	private IObjectFactory of;
	private Random r = new Random();
	private final int id;
	
	public Worker(IObjectFactory of,int i){
		this.of = of;
		this.id = i;
	}
	
	@Override
	public void run() {
		ISimpleRpc sayHello = of.get(ISimpleRpc.class);
		JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
		
		for(;;){
			try {
				String result = sayHello.hello(" Hello LOG: "+id);
				System.out.println(JMicroContext.get().getString(JMicroContext.LINKER_ID, "")+": "+result);
				JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(r.nextInt(200));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
