package cn.jmicro.example.test.ha;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import cn.jmicro.api.masterelection.LegalPerson;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestHA extends JMicroBaseTestCase{

	private void doServerWorker() {
		
		AtomicBoolean finish = new AtomicBoolean(false);
		LegalPerson lp = new LegalPerson(of,"testElection",(type,isMaster)->{
			System.out.println("Election result: " + type + ", isMaster: " + isMaster);
			finish.set(true);
		});
		
		while(!finish.get()) {
			System.out.println("Status: " + lp.isLockStatu() + ", isMaster: " + lp.isMaster());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Test
	public void testCreateSeqNode() throws IOException {
		
		new Thread(this::doServerWorker).start();
		new Thread(this::doServerWorker).start();
		new Thread(this::doServerWorker).start();
		
		this.waitForReady(100000000);
		
	}
	
	
}
