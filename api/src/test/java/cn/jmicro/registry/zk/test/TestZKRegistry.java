package cn.jmicro.registry.zk.test;

import java.util.HashMap;

import org.junit.Test;

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.impl.RegistryImpl;
import cn.jmicro.common.Utils;

public class TestZKRegistry {
	
	/**
	 * 
	cn.jmicro.example.api.ITestRpcService####host%3D192.168.3.3%26port%3D59429
	%26namespace%3DdefaultNamespace%26version%3D0.0.0%26time%3D362551174074939239
	 */
	@Test
	public void testServiceAdd() {
		Config.parseArgs(new HashMap<>());
		RegistryImpl r = new RegistryImpl();
		r.init();
		
		r.addServiceListener("cn.jmicro.example.api.ITestRpcService##defaultNamespace##0.0.0", 
				(type,si)->{
			System.out.println(type);
			System.out.println(si.path(Config.getRaftBasePath(Config.ServiceRegistDir)));
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testNodeCreate() {
		Config.parseArgs(new HashMap<>());
		IObjectFactory of = EnterMain.getObjectFactory();
		of.get(IDataOperator.class).addNodeListener("/jmicro/config/test",
			(type,path,data)->{
				System.out.println(type);
				System.out.println(path);
				System.out.println(data);
			});
		Utils.getIns().waitForShutdown();
	}
	
}
