package org.jmicro.example.test;

import java.util.Date;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.net.DumpManager;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.util.DateUtils;
import org.junit.Test;

public class TestDumpManager {

	public static String dir = "D:\\opensource\\github\\dumpdir";
	
	public static String clientPath = dir+"jmicro.example\\jmicro.example.comsumer\\comsumer-201812190718.dump";
	public static String serverPath = dir+"jmicro.main/jmicro.main.monitor.exception/ServiceReqMonitor-18-12-19-05-50.dump";
	
	public static String outputPath = dir+"jmicro.main/jmicro.main.monitor.exception/result-"+DateUtils.formatDate(new Date(), "YYYYMMddHHmm");;
	
	@Test
	public void testParseDump() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testParseDump","-Dclient=true"});
		JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, true);
		DumpManager.getIns().printByLinkId(clientPath, -1);
	}
	
	@Test
	public void testDumpToFile() {
		DumpManager.getIns().reqResp("test01.txt","lid","intrest");
	}
}
