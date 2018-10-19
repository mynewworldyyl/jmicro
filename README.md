# JMicro说明
1. 以简单方式实现微服务相关功能，包括服务注册，服务发现，服务监控，超时，重试，限速，降级，熔断，负载均衡等
2. 模块之间确保最底偶合度，非常易于扩展，参考jmicro.api，基本上每个接口都只有一个方法
3. 完全基于Java注解声明组件，没有任何XML，property之类的配置;
4. 为微服务定制的极其轻量IOC容器，目前代码大概1500行左右;
5. 监控埋点，可以详细监控服务每个细节，如每个RPC方法响应时间，异常次数，qps等等，并且监控的点非常易于替换或扩展;
6. 如果你喜欢，可以0配置启N个服务，但可以实时修改每个服务方法的配置，并且实时生效；
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

//service说前这是一个RPC服务，指定超时时间是10秒，最高QPS是1个/每秒，s表示秒
@Service(timeout=10*1000,maxSpeed="1s")
@Component //告诉IOC容器这是一个组件
public class TestRpcServiceImpl implements ITestRpcService{

	private AtomicInteger ai = new AtomicInteger();
	
	@Cfg("/limiterName")//从配置中心拿配置，可实时修改并实时生效
	private String name;
	
	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	@SMethod(monitorEnable=1)//服务方法，定义此方法默认可以被监控
	public Person getPerson(Person p) {
		p.setUsername("Server update username");
		p.setId(ai.getAndIncrement());
		System.out.println(p);
		return p;
	}

	@Override
	@SMethod(needResponse=false)//此服务方法不需要响应，请区别于没有返回值的情况，两者不一样
	public void pushMessage(String msg) {
		System.out.println("Server Rec: "+ msg);
	}
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@Override
	//流式RPC，一个请求会收到N个响应，stringMessageCallback是客户端定义的一个普通组件
	//用于接收异步消息
	@SMethod(streamCallback="stringMessageCallback",timeout=10*60*1000)
	public void subscrite(String msg) {
		//从上下文中拿发送接口，可以给客户端推送消息
		IWriteCallback sender = JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK, null);
		if(sender == null){
			throw new CommonException("Not in async context");
		}
		for(int i = 100; i > 0; i++) {
			try {
			     //模拟业务逻辑处理
				Thread.sleep(1000*2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String msg1 = "Server return: "+ count.getAndIncrement()+",msg: " +msg;
			System.out.println(msg1);
			//给客户端推送消息
			sender.send(msg1);
		}
	}
}

~~~

# 启动服务

~~~

public class ServiceProvider {

	public static void main(String[] args) {
		//此行代码启动RPC服务
		JMicro.getObjectFactoryAndStart(args);
		//此行代码只是让主线程停止不退出
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
		//客户端获取IOC容器
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		
		//got remote service from object factory
		//直接从IOC容器中取得远程服务
		ITestRpcService src = of.get(ITestRpcService.class);
		//invoke remote service
		//调用完程服务
		System.out.println(src.hello("Hello JMicro"));
	}
}
~~~

#  JMICRO参考文档

##  注解

##  IOC容器

##  服务端发布RPC服务

##  客户端获取RPC服务

##  动态修改线上服务配置

##  服务监控

##  基于Socket的RPC传输

##  基于HTTPRPC传输

##  服务注册表

##  负载圴行

##  限流

##  降级

##  熔断

##  超时及重试

##  编码解码

##  服务拦截器

##  服务检测

##  全局ID标识

##  。。。



