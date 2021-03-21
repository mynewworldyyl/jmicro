package cn.expjmicro.example.comsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.api.rpc.ISimpleRpc;
import cn.expjmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.api.test.Person;

@Component(lazy=false, level=9999)
public class ServiceComsumer implements IPostFactoryListener{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceComsumer.class);
	
	@Override
	public void afterInit(IObjectFactory of) {

		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		//IObjectFactory of = (IObjectFactory)EnterMain.getObjectFactoryAndStart(args);
		//JMicroContext.get().setParam("routerTag", "tagValue");
		
		//got remote service from object factory
		//ISimpleRpc src = of.getRemoteServie(ISimpleRpc.class,null);
		LG.log(MC.LOG_DEBUG, ServiceComsumer.class, "test submit nonrpc log");
		//ISimpleRpc src = of.get(ISimpleRpc.class);
		ISimpleRpc$JMAsyncClient src = (ISimpleRpc$JMAsyncClient)of
				.getRemoteServie(ISimpleRpc.class, "exampleProdiver", null);
		//invoke remote service
		System.out.println(src.hi(new Person(22,"www")));
		/*src.helloJMAsync("Hello JMicro")
		.then((rst, fail,ctx)->{
			System.out.println(rst);
		});*/
		
	  /* src.linkRpc("Client to call linkRpc")
		.then((rst, fail,ctx)->{
			System.out.println(rst);
		});*/
		
		/*src.linkRpcAs("test out linkRpcAs")
		.success((rst,cxt)->{
			System.out.println(rst);

			src.linkRpcAs("inner linkRpcAs0")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
			src.linkRpcAs("inner linkRpcAs1")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
			src.linkRpcAs("inner linkRpcAs2")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
		})
		.fail((code,msg,cxt)->{
			System.out.println("code: " + code +", msg: " + msg);
		});
		*/
		
		try {
			Thread.sleep(1000*30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	
	}

	@Override
	public int runLevel() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void preInit(IObjectFactory of) {
		
	}
	
}
