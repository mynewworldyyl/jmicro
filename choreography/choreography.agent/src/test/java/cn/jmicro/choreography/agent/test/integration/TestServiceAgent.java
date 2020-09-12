package cn.jmicro.choreography.agent.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cn.jmicro.api.Resp;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.IAgentProcessService;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestServiceAgent extends JMicroBaseTestCase {

	@Test
	public void testIAgentProcessService() throws InterruptedException {
		
		IAgentProcessService ms = of.get(IAgentProcessService.class);
		
		Boolean resp = ms.startLogMonitor("450", "output.log", 1);
		
		System.out.println(resp);
		
		this.waitForReady(Long.MAX_VALUE);
	}
	
	@Test
	public void testStartLocalProcess() {
		
		//Process process = Runtime.getRuntime().exec("java -jar ProcessJar.jar args1 agrs2 args3");
		
		List<String> list = new ArrayList<String>();
		
		String workDir = "D:\\opensource\\github\\jmicro\\jmicro.choreography\\jmicro.choreography.agent\\";
		
		list.add("java");
		
		list.add("-javaagent:" + workDir + "resourceDir\\jmicro.agent-0.0.1-RELEASE.jar");
		
		list.add("-jar");
		list.add(workDir + "resourceDir\\jmicro.pubsub-0.0.1-RELEASE-jar-with-dependencies.jar");
		
		
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
