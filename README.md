# JMicro说明
1. 以简单方式实现微服务相关功能，包括服务注册，服务发现，服务监控，超时，重试，限速，降级，熔断，负载均衡等
2. 模块之间确保最底偶合度，非常易于扩展，参考jmicro.api，基本上每个接口都只有一个方法
3. 完全基于Java注解声明组件，没有任何XML，property之类的配置;
4. 为微服务定制的极其轻量IOC容器，目前代码大概1500行左右;
5. 监控埋点，可以详细监控服务每个细节，如每个RPC方法响应时间，异常次数，qps等等，并且监控的点非常易于替换或扩展;
6. 如果你喜欢，可以0配置启N个服务，但实时修改每个服务方法的配置，并且实时生效；
7. 简单一致的HTTP支持，可以接入任何客户端；
8. 每个请求，连接，消息有全局唯一标识，实现整个请求的全流程串连监控；
9. 运行jmicro.example样例，体验基于JMicro开发服务有多简单；
10. 可选通过线程和协程做主请求分发；
11. 客户端一个请求多个响应，类似发布订阅
10. 更多功能会持继增加

# 下载源代码
git checkout https://github.com/mynewworldyyl/jmicro.git

# 构建
maven clean install

# 启动Zookeeper，很快将会增加ETCD支持，到时性能将会有质的提高
 run zookeeper 

# 定义一个服务,完整代码请参考jmicro.example下面的子项目
~~~

package org.jmicro.example.api;

@Service
public interface ITestRpcService {
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	void subscrite(String msg);
	
	String hello(String name);
	
}

~~~

# 实现服务
~~~

package org.jmicro.example.provider;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.Person;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.server.IWriteCallback;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.example.api.ITestRpcService;

@Service(timeout=10*60*1000,maxSpeed="1s")
@Component
public class TestRpcServiceImpl implements ITestRpcService{

	private AtomicInteger ai = new AtomicInteger();
	
	@Cfg("/limiterName")
	private String name;
	
	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	@SMethod(monitorEnable=1)
	public Person getPerson(Person p) {
		p.setUsername("Server update username");
		p.setId(ai.getAndIncrement());
		System.out.println(p);
		return p;
	}

	@Override
	@SMethod(needResponse=false)
	public void pushMessage(String msg) {
		System.out.println("Server Rec: "+ msg);
	}
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@Override
	@SMethod(streamCallback="stringMessageCallback",timeout=10*60*1000)
	public void subscrite(String msg) {
		IWriteCallback sender = JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK, null);
		if(sender == null){
			throw new CommonException("Not in async context");
		}
		for(int i = 100; i > 0; i++) {
			try {
				Thread.sleep(1000*2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String msg1 = "Server return: "+ count.getAndIncrement()+",msg: " +msg;
			System.out.println(msg1);
			sender.send(msg1);
		}
	}
}

## 启动服务

~~~

public class ServiceProvider {

	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		Utils.getIns().waitForShutdown();
	}

}

~~~

# 客户使用服务

~~~

package org.jmicro.example.comsumer;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.example.api.ITestRpcService;

public class ServiceComsumer {
	public static void main(String[] args) {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		
		//got remote service from object factory
		ITestRpcService src = of.get(ITestRpcService.class);
		//invoke remote service
		System.out.println(src.hello("Hello JMicro"));
	}
}

~~~