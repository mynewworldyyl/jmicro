package cn.expjmicro.example.test.ha;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.masterelection.VoterPerson;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestHA extends JMicroBaseTestCase{

	private void doServerWorker() {
		
		AtomicBoolean finish = new AtomicBoolean(false);
		
		IMasterChangeListener lis = (type,isMaster)->{
			System.out.println("Election result: " + type + ", isMaster: " + isMaster);
			finish.set(true);
		};
		
		VoterPerson lp = new VoterPerson(of,"testElection");
		lp.addListener(lis);
		
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
