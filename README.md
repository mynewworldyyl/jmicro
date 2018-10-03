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

# start comsumer below

~~~
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
	}
}
~~~
