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


# start service provider code below
~~~
public class ServiceProvider {
	 public static void main(String[] args) {
		Config cfg = new Config();
		cfg.setBindIp("localhost");
		cfg.setPort(9800);;
		cfg.setBasePackages(new String[]{"org.jmicro","org.jmtest"});
		cfg.setRegistryUrl(new URL("zookeeper","localhost",2180));
		JMicroContext.setCfg(cfg);
		ComponentManager.getObjectFactory();
		Utils.waitForShutdown();
	}
}
~~~

# start comsumer below

~~~
public class ServiceComsumer {

	public static void main(String[] args) {
		Config cfg = new Config();
		cfg.setBindIp("localhost");
		cfg.setPort(9801); //yes client can be a service if you need
		cfg.setBasePackages(new String[]{"org.jmicro","org.jmtest"}); 
		cfg.setRegistryUrl(new URL("zookeeper","localhost",2180));
		JMicroContext.setCfg(cfg);
		//got remote service from object factory
		TestRpcClient src = ComponentManager.getObjectFactory().get(TestRpcClient.class);
		//invoke remote service
		src.invokePersonService();
	}
}
~~~
