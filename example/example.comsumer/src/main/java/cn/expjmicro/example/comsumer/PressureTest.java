package cn.expjmicro.example.comsumer;

import java.util.Random;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.test.Person;
import cn.expjmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;

public class PressureTest {

	public static void main(String[] args) {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
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
		singleRpc();
		//linkRpc();
	}
	
	private void linkRpc() {

		ISimpleRpc$JMAsyncClient sayHello = of.getRemoteServie(ISimpleRpc$JMAsyncClient.class.getName(),"exampleProdiver","0.0.1", null);
		JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
		
		for(;;){
			try {
				/*
				String result = sayHello.hello("Hello LOG: "+id);
				System.out.println(JMicroContext.get().getString(JMicroContext.LINKER_ID, "")+": "+result);
				JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
				 */
				sayHello.linkRpc("Hello LOG: "+id)
				.fail((code,result,cxt)->{
					String msg = JMicroContext.get().getLong(JMicroContext.LINKER_ID, 0L)+": "+result;
					//System.out.println(msg);
					LG.log(MC.LOG_ERROR, PressureTest.class, msg);
					JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
				}).success((result,cxt)->{
					LG.log(MC.LOG_INFO, PressureTest.class, result);
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(r.nextInt(5000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	
	private void singleRpc() {

		ISimpleRpc$JMAsyncClient sayHello = of.getRemoteServie(ISimpleRpc$JMAsyncClient.class.getName(),"exampleProdiver","0.0.1", null);
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
						LG.log(MC.LOG_ERROR, PressureTest.class, code+":"+result);
						JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
					}).success((result,cxt)->{
						LG.log(MC.LOG_DEBUG, PressureTest.class, result);
						//System.out.println("Result: " +result);
					});
				}else {
					sayHello.hiJMAsync(new Person())
					.fail((code,result,cxt)->{
						LG.log(MC.LOG_ERROR, PressureTest.class, code+":"+result);
						JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
					}).success((result,cxt)->{
						LG.log(MC.LOG_DEBUG, PressureTest.class, "Result: " +result);
						//System.out.println("Result: " +result);
					});
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(r.nextInt(5000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
	}
	
}
