# JMicro说明
1. 以简单方式实现微服务相关功能，包括服务注册，服务发现，服务监控，超时，重试，限速，降级，熔断，负载均衡等
2. 模块之间确保最底偶合度，非常易于扩展，参考jmicro.api，基本上每个接口都只有一个方法
3. 完全基于Java注解声明组件，没有任何XML，property之类的配置;
4. 为微服务定制的极其轻量IOC容器，目前代码大概1500行左右;
5. 监控埋点，可以详细监控服务每个细节，如每个RPC方法响应时间，异常次数，qps等等，并且监控的点非常易于替换或扩展;
6. 如果你喜欢，可以0配置启N个服务，但可以实时修改每个服务方法的配置，并且实时生效；
7. 简单一致的HTTP支持，这样就可以以HTTP方式接入任何客户端；
8. 每个请求，连接，消息有全局唯一标识，实现整个请求的全流程串连监控；
9. 运行jmicro.example样例，体验基于JMicro开发服务有多简单；
10. 基本上无同步块代码（当然前提能保证线程安全），确保框架不存在性能瓶颈；
11. 可选通过线程和协程（FIBER）做请求分发；
12. 客户端一个请求多个响应，类似订阅
13. 更多功能会持继增加

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

以下如果没有说明的字段，默认是还没实现，或实现还不切底，后面实现后会持续更新

##  Service，SMethod，Component，Inject，Reference注解是框架使用者所关注的5个注解，别的都可以不用了解，下面重点说明这3个注解。
需要说明的是，下面定义的属性都是系统启动时的默认值，启动后也可以在配置中心或通过API接口动态修改，实时生效
Service和SMethod是服务实现方使用，
Component和Inject服务实现方及使用方都可以使用
Reference服务使用方使用

服务开发者或使用者了解了这5个注解的使用方法后，就完全可以做服务开发或使用服务，其他都是深入了解相关。


### Service 标识这个类是一个服务，系统自动检测服务接口并注册相关信息到注册中心

~~~
@Target({TYPE})
@Retention(RUNTIME)
public @interface Service {

	public String value() default "";
	
	/**
	 * 服务使用的注册表，如将来同时使用实现ZK或Etcd，此属性目前感觉没必要，一个就足够了，
	 * 真想不明白同时用两种类型注册表有什么用
	 */
	public String registry() default Constants.DEFAULT_REGISTRY;
	
	/**
	 * 底层传输层，可以是http或Mina
	 */
	public String server() default Constants.DEFAULT_SERVER;
	
	/**
	 * 服务接口，如果类只实现一个接口，则此值可不填
	 * 一个实现只能存在一个接口作为服务，如果类同时实现了多个接口，则需要在此属性说明那个接口是服务接口
	 */
	public Class<?> infs() default Void.class;
	
	/**
	 * 服务命名空间，服务之间以命名空间作为区别，如出库单服务，入库单服务可以用不同的命名空间相区别，利于服务管理
	 * 客户端使用服务时，可以指定命名空间
	 */
	public String namespace() default Constants.DEFAULT_NAMESPACE;

	/**
	 * 服务版本，每个服务接口可以有多个版本，版本格式为 DD.DD.DD,6个数字用英方步点号隔开
	 * 客户端使用服务时，可以指定版本或版本范围
	 */
	public String version() default Constants.DEFAULT_VERSION;
	
	/**
	 * 服务是否可监控，-1表示未定义，由别的地方定义，如系统环境变量，启动时指定等，0表示不可监控，1表未可以被监控
	 * 可以被监控的意思是：系统启用埋点日志上报，服务请求开始，服务请求得到OK响应，服务超时，服务异常等埋点
	 */
	public int monitorEnable() default -1;
	
	/**
	 * 如果超时了，要间隔多久才重试
	 * @return
	 */
	public int retryInterval() default 500;
	/**
	 * 重试次数
	 */
	//method must can be retry, or 1
	public int retryCnt() default 3;
	
	/**
	 * 请求超时，单位是毫秒
	 */
	public int timeout() default 2000;
	
	/**
	 * 系统检测自动带上的参数 
	 */
	public String testingArgs() default "";
	
	/**
	 * 服务降级前最大失败次数，如降底QPS，提高响应时间等策略
	 * @return
	 */
	public int maxFailBeforeDegrade() default 100;
	
	/**
	 * 可以接受的最大平均响应时间，如果监控检测到超过此时间，系统可能会被降级或熔断
	 */
	public int avgResponseTime() default -1;
	
	/**
	 * 服务熔断前最大失败次数
	 * @return
	 */
	public int maxFailBeforeFusing() default 500;
	
	/**
	 * 支持的最高QPS
	 */
	public String maxSpeed() default "";
}
~~~

### SMethod 注解在服务方法上，除以下两个字段外，其他属性与Service注解相同，只是方法注解具有最高优先极。

~~~
	//0: need response, 1:no need response
	public boolean needResponse() default true;
	
	/**
	 * 实现IMessageCallback接口的组件名称，用于处理异步消息
	 */
	// StringUtils.isEmpty()=true: not stream, false: stream, one request will got more response
	// if this value is not NULL, the async is always true without check the real value
	// value is the callback component in IOC container created in client
	public String streamCallback() default "";

~~~

### Component 声明类是一个组件，IOC容器启动时生成组件的唯一实例，并且确保在此IOC容器唯一实例。
~~~
@Target(TYPE)
@Retention(RUNTIME)
public @interface Component {
	/**
	 * 组件名称，必须确保在全局唯一 
	 */
	public String value() default "";
	/**
	 * 使用时才实例化，启动时只是生成代理
	 */
	public boolean lazy() default true;
	
	/**
	 * 实例化优先级，值越底，优先极越高。用户自定义的服务因为依赖于系统的核心组件，所以用户自定义的组件的level值不要太小，建议从10000开始
	 * 如果用户定义的组件A和组件B，B依赖于A，则A的level要大于B，否则B先于A启动，B的依赖没有找到，从而报错
	 */
	public int level() default 10000;
	/**
	 * 组件是否可用，如当前开发了实现相同功能的服务A和B，但是此时不想启用A，可以暂时设置active=false，则IOC容器不会实例化A。
	 * @return
	 */
	public boolean active() default true;
	
	//provider or client or NULL witch can be used any side
	/**
	 * 此组件的使用方，可以是服务提供方或消费方，也可以两方都可以使用，
	 * 如果指定了服务提供方或消费方，则该组件所依赖的组件也被限制为指定方
	 * @return
	 */
	public String side() default Constants.SIDE_ANY; 
}

~~~

### Demo

~~~
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
~~~

### 服务使用方除Component注解外，还有Reference注解在字段上，声明依赖远程RPC服务

~~~
@Target(FIELD)
@Retention(RUNTIME)
public @interface Reference {
	public String value() default "";
	/**
	 * 依赖的服务名称空间，如果指定了名称空间，则只有与此名称空间相同的服务才会被注入到此字段
	 * 如果目前有两个服务实现相同的服务接口，名称空间相同或没指定名称空间，系统无法确定要注入那个服务，则会报错，
	 * 此时应该指定名称空间
	 * @return
	 */
	public String namespace() default "";
	
	/**
	 * 服务版本，使用原理和名称空间相同，版本是同一个名称空间下同一个接口的不同实现版本
	 * @return
	 */
	public String version() default "";
	
	/**
	 * 此依赖是否是必须的，如果是必须的，但是启动时注册中心又没有此服务，则报错
	 * @return
	 */
	public boolean required() default false;
	
	public String registry() default "";
	
	/**
	 * 依赖服务有变化时，包括配置及服务上线下线的变化，则会调用此字段值对应的组件方法，让组件
	 * 对服务变化作出响应
	 * @return
	 */
	public String changeListener() default "";
}
~~~

Demo

~~~
@Component
public class TestRpcClient {

    //注入远程RPC服务，服务可以还不存在
	@Reference(required=false)
	private ITestRpcService rpcService;
	
	 //注入远程RPC服务，服务必须启动时存在可用
	@Reference(required=true)
	private ISayHello sayHello;
	
	public void invokeRpcService(){
		String result = sayHello.hello("Hello RPC Server");
		System.out.println("Get remote result:"+result);
	}
	
	public void invokePersonService(){
		Person p = new Person();
		p.setId(1234);
		p.setUsername("Client person Name");
		//如果此时服务还不存在，则会报错了
		p = rpcService.getPerson(p);
		System.out.println(p.toString());
	}
}
~~~


### Inject注解，注入IOC容器的依赖

~~~
@Target(FIELD)
@Retention(RUNTIME)
public @interface Inject {

	public String value() default "";
	public boolean required() default true;
	
	/**
	 * if true inject remote services and local component that implement the same interface,
	 * if false ,only inject local component.
	 * the reference annotation only inject remote services
	 */
	public boolean remote() default false;
}
~~~

DEMO

~~~
@Component(lazy=false)
public class ProxyObjectForTest {
	/*private Object[] conArgs;
	private String conKey;
	public ProxyObject(Object[] $args){this.conArgs=$args; for(Object arg: $args) { this.conKey = this.conKey + arg.getClass().getName(); } }*/
	
	@Inject //自动注入服务注册服，IRegistry是系统实现的本地组件，默认可以使用
	private IRegistry registry;
	
	private String msg = "ProxyObjectForTest";
	
	public ProxyObjectForTest(String msg){
		this.msg = msg;
	}
	
	public ProxyObjectForTest(){}
	
	public void invokeRpcService(){
		System.out.println("invokeRpcService: "+this.msg);
	}
	
	public void invokeRpcService1(){
		System.out.println("invokeRpcService1: "+this.msg);
	}
	
}

~~~


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



