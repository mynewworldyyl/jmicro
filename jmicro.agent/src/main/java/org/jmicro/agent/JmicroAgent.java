package org.jmicro.agent;

import java.lang.instrument.Instrumentation;

public class JmicroAgent {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new AddSerializedToObject(),false);
	}
}
