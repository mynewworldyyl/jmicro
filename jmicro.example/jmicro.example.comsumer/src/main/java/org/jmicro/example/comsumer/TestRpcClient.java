package org.jmicro.example.comsumer;

import java.util.HashSet;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.jmicro.api.annotation.Async;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.test.Person;
import org.jmicro.common.CommonException;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.example.api.rpc.ISimpleRpc;

@Component(level=1000)
public class TestRpcClient {

	@Reference(required=true,namespace="testrpc",version="0.0.1")
	private ITestRpcService rpcService;
	
	@Reference(required=false,namespace="simpleRpc",version="0.0.1")
	private ISimpleRpc sayHello;
	
	@Reference(required=true,namespace="simpleRpc",version="0.0.1")
	private Set<ISimpleRpc> services = new HashSet<>();
	
	@Reference(required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber> submiters = new HashSet<>();
	
	@Reference(required=true,namespace="simpleRpc",version="0.0.1",
			asyncs= {
					@Async(forMethod="hello",enable=true,condition=AsyncConfig.ASYNC_DIRECT,
				serviceName="org.jmicro.example.api.rpc.IAsyncRpcCallback",
				namespace="asyncRpcCallback",
				version="0.0.1",method="callback")
			})
	private ISimpleRpc asyncRpc;
	
	@Inject
	private CuratorFramework curator;
	
	public void invokeRpcService(){
		String result = sayHello.hello("Hello RPC Server");
		System.out.println("Get remote result:"+result);
	}
	
	public void invokePersonService(){
		Person p = new Person();
		p.setId(1234);
		p.setUsername("Client person Name");
		p = rpcService.getPerson(p);
		System.out.println(p.toString());
	}

	public Set<IMonitorDataSubscriber> getSubmiters() {
		return submiters;
	}
	
	public String testSetServices() {
		if(services.isEmpty()) {
			throw new CommonException("SayHello Set is NULL");
		}
		return services.iterator().next().hello("testSetServices");
	}
	
	public void testCallAsyncRpc() {
		asyncRpc.hello("Hello world");
	}
}
