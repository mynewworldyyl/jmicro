package org.jmicro.common.test;

import java.util.List;

import org.jmicro.common.Utils;
import org.junit.Test;

public class TestClass {

	@Test
	public void testGetLocalIp(){
		 List<String> ips = Utils.getIns().getLocalIPList();
		 System.out.println(ips);
	}
	
	
}
