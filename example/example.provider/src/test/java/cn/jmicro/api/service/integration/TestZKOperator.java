package cn.jmicro.api.service.integration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestZKOperator extends JMicroBaseTestCase{

	private final static Logger logger = LoggerFactory.getLogger(TestZKOperator.class);
	
	@Test
	public void testAddServiceListener() {
		IDataOperator op = this.get(IDataOperator.class);
		 op.addChildrenListener(Config.InstanceDir,
				(type,parent,child,data) -> {
					System.out.print("\n");
					if(type == IServiceListener.ADD) {
						System.out.print(data+"\n");
					}else if(type == IServiceListener.REMOVE) {
						System.out.print(child+"\n");
					}
			});
			
		 JMicro.waitForShutdown();
		}
	
	
}
