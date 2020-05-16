package org.jmicro.choreography.agent.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jmicro.api.choreography.ChoyConstants;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestServiceAgent/* extends JMicroBaseTestCase*/{

	@Test
	public void testStartLocalProcess() {
		
		//Process process = Runtime.getRuntime().exec("java -jar ProcessJar.jar args1 agrs2 args3");
		
		List<String> list = new ArrayList<String>();
		
		String workDir = "D:\\opensource\\github\\jmicro\\jmicro.choreography\\jmicro.choreography.agent\\";
		
		list.add("java");
		
		list.add("-javaagent:" + workDir + "resourceDir\\jmicro.agent-0.0.1-SNAPSHOT.jar");
		
		list.add("-jar");
		list.add(workDir + "resourceDir\\jmicro.pubsub-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		
		
		list.add("-D"+ChoyConstants.ARG_INSTANCE_ID+"=" + 23);
		list.add("-D"+ChoyConstants.ARG_MYPARENT_ID+"="+1);
		list.add("-D"+ChoyConstants.ARG_DEP_ID+"="+22);
		list.add("-D"+ChoyConstants.ARG_AGENT_ID+"="+33);
		
		ProcessBuilder pb = new ProcessBuilder(list);
		//pb.redirectErrorStream(true);
		
		File wd = new File(workDir + "agentWorkDir\\test");
		wd.mkdirs();
		pb.directory(wd);
		
		File errorFile = new File(wd,"error.log");
		pb.redirectError(errorFile);
		
		File outputFile = new File(wd,"nohup.log");
		pb.redirectOutput(outputFile);
		
		try {
			Process p = pb.start();
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		//waitForReady(30*60);
	}
}
