package cn.jmicro.example.test;

import java.util.Date;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.net.DumpManager;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.utils.DateUtils;

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
