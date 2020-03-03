package org.jmicro.api.service.integration;

import org.jmicro.api.JMicro;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
