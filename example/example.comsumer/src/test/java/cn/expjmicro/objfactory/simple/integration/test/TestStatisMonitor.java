package cn.expjmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestStatisMonitor extends JMicroBaseTestCase{
	
	@Test
	public void testSubmitRpcItem() {
		StatisMonitorClient m = of.get(StatisMonitorClient.class);
		m.readySubmit(rpcStatisItem(MC.MT_REQ_START,Config.getClientId()));
		this.waitForReady(1000000);
	}
	
	@Test
	public void testSubmitNonRpcItem() {
		int cnt=10;
		for(;cnt-->0;) {
			StatisMonitorClient m = of.get(StatisMonitorClient.class);
			m.readySubmit(nonRpcStatisItem(MC.MT_SERVER_STOP));
			waitForReady(1);
		}
		//this.waitForReady(1000000);
	}
	
	@Test
	public void testSubmitNonRpcItemWithMT() {
		int cnt=10;
		for(;cnt-->0;) {
			MT.nonRpcEvent(MC.MT_SERVER_STOP);
			waitForReady(1);
		}
		
		this.waitForReady(60);
	}
	
	private JMStatisItem nonRpcStatisItem(Short type) {
		JMStatisItem si = new JMStatisItem();
		
		si.setRpc(false);
		si.setClientId(Config.getClientId());
		si.setInstanceName(Config.getInstanceName());
		si.setKey(null);
		si.setSubmitTime(TimeUtils.getCurTime());
		si.setLocalHost(Config.getExportSocketHost());
		si.setRemoteHost(Config.getExportSocketHost());
		si.setLocalPort("8883");
		si.setRemotePort("8886");
		
		si.addType(type, 1);
		
		return si;
	}
	
	private JMStatisItem rpcStatisItem(Short type,int clientId) {
		JMStatisItem si = new JMStatisItem();
		
		si.setRpc(true);
		si.setClientId(clientId);
		si.setInstanceName(Config.getInstanceName());
		si.setKey(sayHelloServiceMethod().getKey().toKey(true, true, true));
		si.setSubmitTime(TimeUtils.getCurTime());
		si.setLocalHost(Config.getExportSocketHost());
		si.setRemoteHost(Config.getExportSocketHost());
		si.setLocalPort("8883");
		si.setRemotePort("8886");
		
		si.addType(type, 1);
		
		return si;
	}
}
