package org.jmicro.api.codec;

import org.jmicro.common.Utils;
import org.junit.Test;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;

public class TestFiber {

	@SuppressWarnings("serial")
	@Test
	public void helloFiber() {
		new Fiber<String>() {
			@Override
			protected String run() throws SuspendExecution, InterruptedException {
				System.out.println("Hello Fiber");
				return "Hello Fiber";
			}
			
		}.start();
		
		Utils.getIns().waitForShutdown();
	}
}
