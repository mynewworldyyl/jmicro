package org.jmicro.zk.test;

import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.common.Utils;
import org.jmicro.zk.ZKDataOperator;
import org.junit.Test;

public class TestZKDataOperator {
	
	@Test
	public void testGetData() {
		Config.parseArgs(new String[0]);
		System.out.println(ZKDataOperator.getIns().getData("/jmicro/config/basePackages"));
	}
	
	@Test
	public void testGetDataListener() {
		Config.parseArgs(new String[0]);
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
		Config.parseArgs(new String[0]);
		ZKDataOperator.getIns().addChildrenListener("/jmicro/config",
			(type,path,child,data)->{
				System.out.println(path);
				System.out.println(child);
			});
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testNodeCreate() {
		Config.parseArgs(new String[0]);
		ZKDataOperator.getIns().addNodeListener("/jmicro/config/test",
			(type,path,data)->{
				System.out.println(type);
				System.out.println(path);
				System.out.println(data);
			});
		Utils.getIns().waitForShutdown();
	}
	
}
