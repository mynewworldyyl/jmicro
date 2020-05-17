package cn.jmicro.common.test;

import java.util.List;

import org.junit.Test;

import cn.jmicro.common.Utils;

public class TestClass {

	@Test
	public void testGetLocalIp(){
		 List<String> ips = Utils.getIns().getLocalIPList();
		 System.out.println(ips);
	}
	
}
