package org.jmicro.api.test.monitor;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.monitor.ServiceCounter;
import org.junit.Test;

public class TestServiceCounter {

	@Test
	public void testServiceCounter() {
		Random r = new Random(1000);
		ServiceCounter count = new ServiceCounter("test", 10, TimeUnit.SECONDS);
		count.addCounter(1, 10);
		for(;true;) {
			count.add(1, 1);
			System.out.println("Total:"+count.get(1));
			System.out.println("Avg:"+count.getAvgWithEx(1, TimeUnit.SECONDS));
			System.out.println("=============================");
			try {
				int t = r.nextInt(1000);
				Thread.sleep((t < 0 ? -t : t));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
