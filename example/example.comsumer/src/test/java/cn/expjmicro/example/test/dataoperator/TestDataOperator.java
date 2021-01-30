package cn.expjmicro.example.test.dataoperator;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.VoterPerson;
import cn.jmicro.api.monitor.ILogMonitorServer;
import cn.jmicro.api.monitor.ILogWarning;
import cn.jmicro.api.monitor.LogWarningConfig;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestDataOperator extends JMicroBaseTestCase {
	
	@Test
	public void testCreateSeqNode() throws IOException {
		IDataOperator op = of.get(IDataOperator.class);
		op.createNodeOrSetData(VoterPerson.ROOT+"/testElNode/tag", "", IDataOperator.EPHEMERAL_SEQUENTIAL);
		waitForReady(30*60);
	}
	
	@Test
	public void testDeleteAllServiceItems() throws IOException {
		IDataOperator op = of.get(IDataOperator.class);
		Set<String> chs = op.getChildren(Config.ServiceRegistDir, false);
		for(String c : chs) {
			op.deleteNode(Config.ServiceRegistDir+"/" + c);
		}
	}
	
	@Test
	public void testSaveLogWarningConfig() throws IOException {
		IDataOperator op = of.get(IDataOperator.class);
		
		LogWarningConfig cfg = new LogWarningConfig();
		cfg.setClientId(0);
		cfg.setExpStr("level >= "+MC.LOG_DEBUG);
		cfg.setId("0");
		//cfg.setLevel(MC.LOG_DEBUG);
		cfg.setMinNotifyInterval(1000);
		
		String sm = ILogWarning.class.getName() + UniqueServiceKey.SEP +
				"logmonitor" + UniqueServiceKey.SEP +
				"0.0.1" + UniqueServiceKey.SEP +
				"" + UniqueServiceKey.SEP +
				"" + UniqueServiceKey.SEP +
				"" + UniqueServiceKey.SEP +
				"warn" + UniqueServiceKey.SEP + MRpcLogItem.class.getName();
				
		cfg.setCfgParams(sm);
		
		String data = JsonUtils.getIns().toJson(cfg);
		op.createNodeOrSetData(ILogMonitorServer.LOG_WARNING_ROOT+"/0", data, false);
		
		waitForReady(30*60);
	}
	
}
