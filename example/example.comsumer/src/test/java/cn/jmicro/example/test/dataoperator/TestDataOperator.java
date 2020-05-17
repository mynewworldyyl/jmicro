package cn.jmicro.example.test.dataoperator;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.LegalPerson;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestDataOperator extends JMicroBaseTestCase {
	
	@Test
	public void testCreateSeqNode() throws IOException {
		IDataOperator op = of.get(IDataOperator.class);
		op.createNode(LegalPerson.ROOT+"/testElNode/tag", "", IDataOperator.EPHEMERAL_SEQUENTIAL);
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
	
	
	
}
