package cn.jmicro.zk.test;

import java.util.HashMap;

import org.junit.Test;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.common.Utils;
import cn.jmicro.zk.ZKDataOperator;

public class TestZKDataOperator {
	
	@Test
	public void testGetData() {
		Config.parseArgs(new HashMap<>());
		System.out.println(ZKDataOperator.getIns().getData("/jmicro/config/basePackages"));
	}
	
	@Test
	public void testGetDataListener() {
		Config.parseArgs(new HashMap<>());
		ZKDataOperator.getIns().addDataListener("/jmicro/config/monitorClientEnable",
			new IDataListener(){
				@Override
				public void dataChanged(String path, String data) {
					System.out.println(path);
					System.out.println(data);
				}
		});
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testWatchChildren() {
		Config.parseArgs(new HashMap<>());
		ZKDataOperator.getIns().addChildrenListener("/jmicro/config",
			(type,path,child)->{
				System.out.println(path);
				System.out.println(child);
			});
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testNodeCreate() {
		Config.parseArgs(new HashMap<>());
		ZKDataOperator.getIns().addNodeListener("/jmicro/config/test",
			(type,path,data)->{
				System.out.println(type);
				System.out.println(path);
				System.out.println(data);
			});
		Utils.getIns().waitForShutdown();
	}
	
}
