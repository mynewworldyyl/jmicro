package cn.jmicro.example.comsumer;

import java.util.Random;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.test.Person;
import cn.jmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;

public class PressureTest {

	public static void main(String[] args) {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		for(int i = 0; i < 5;i++){
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
		ISimpleRpc$JMAsyncClient sayHello = of.getRemoteServie(ISimpleRpc$JMAsyncClient.class.getName(),"simpleRpc","0.0.1", null);
		JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
		
		for(;;){
			try {
				/*
				String result = sayHello.hello(" Hello LOG: "+id);
				System.out.println(JMicroContext.get().getString(JMicroContext.LINKER_ID, "")+": "+result);
				JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
				 */
				if(r.nextBoolean()) {
					sayHello.helloJMAsync("Hello LOG: "+id)
					.fail((code,result,cxt)->{
						//System.out.println(JMicroContext.get().getLong(JMicroContext.LINKER_ID, 0L)+": "+result);
						JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
					}).success((result,cxt)->{
						//System.out.println("Result: " +result);
					});
				}else {
					sayHello.hiJMAsync(new Person())
					.fail((code,result,cxt)->{
						System.out.println(JMicroContext.get().getLong(JMicroContext.LINKER_ID, 0L)+": "+result);
						JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
					}).success((result,cxt)->{
						//System.out.println("Result: " +result);
					});
				}
				
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(r.nextInt(300));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
