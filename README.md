# jmicro
1. Simple way to implement micro service framework, also very easy to use (VS to dubbo,spring cloud);
2. Easy to extend the core functions, such as loadbalance, transport, registry,codec,timeout,retry,service downgrade and cutdown ...
3. Service monitor and dynamic reload when fail;
4. Real time service data, such as qps for specify service method;
5. IOC Container implement for this framework which only support singleton instance;
6. Configuration management support by ZK or Etcd
7. Sentinel support
8. More functions will be added ....

# downnload source code
git checkout https://github.com/mynewworldyyl/jmicro.git

# build
maven clean install

# start zk registry
 run zookeeper (will support etcd in future)

# define service interface
~~~
public interface ITestRpcService {

	String hello(String name);
	
	Person getPerson(Person p);
}

person class as argument between comsumer and provider

public class Person{
	
	private String username ="";
	private int id = 222;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "ID: " + this.id+", username: " + this.username;
	}
}
~~~

# start service provider code
~~~

## implement service

package org.jmicro.example.provider;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Service;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.example.api.Person;

@Service //will tell the objectfactory this class is a remote service
public class TestRpcServiceImpl implements ITestRpcService{

	@Cfg("/name") //inject by objectfactory and value got from zookeeper
	private String name;
	
	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	public Person getPerson(Person p) {
		System.out.println(p);
		p.setUsername("Server update username");
		p.setId(2222);
		return p;
	}
}

## start the service provider

package org.jmicro.main;

import org.jmicro.api.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Utils;

public class ServiceProvider {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		Utils.waitForShutdown();
	}

}
~~~

# start comsumer
comsumer only dependent the service interface not the implementation
~~~
## Test rpc client
@Component //this will tell objectfactory this class is a component and create instance
public class TestRpcClient {

	@Reference(required=true) // got the remote service
	private ITestRpcService rpcService;
	
	public void invokeRpcService(){
	     //invoke remote service
		String result = rpcService.hello("Hello RPC Server");
		System.out.println("Get remote result:"+result);
	}
	
	public void invokePersonService(){
	 //invoke remote service
		Person p = new Person();
		p.setId(1234);
		p.setUsername("Client person Name");
		p = rpcService.getPerson(p);
		System.out.println(p.toString());
	}
	
}

## start the client in main function
package org.jmicro.main;

import org.jmicro.api.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;

public class ServiceComsumer {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		
		//got remote service from object factory
		TestRpcClient src = of.get(TestRpcClient.class);
		//invoke remote service
		src.invokePersonService();
		
		//actually , you can got remote service directory
		ITestRpcService srv = of.get(ITestRpcService.class);
		srv.hello("Hello RPC Server");
	}
}
~~~
