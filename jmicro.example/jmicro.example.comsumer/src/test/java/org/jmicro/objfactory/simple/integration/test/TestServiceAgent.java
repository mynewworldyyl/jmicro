package org.jmicro.objfactory.simple.integration.test;

import org.jmicro.api.choreography.agent.IServiceAgent;
import org.jmicro.api.choreography.base.SchedulerResult;
import org.jmicro.api.choreography.controller.InstanceManager;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.Test;

public class TestServiceAgent  extends JMicroBaseTestCase{

	@Test
	public void testStartService() {
		
		IServiceAgent psm = of.getRemoteServie(IServiceAgent.class.getName(), "choreography.agent", "0.0.1",null);
		String processId = of.get(ComponentIdServer.class).getStringId(IServiceAgent.class);
		SchedulerResult result = psm.startService(processId,"D:\\opensource\\github\\jmicro\\jmicro.main\\jmicro.main.monitor.exception\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.zk\\target\\classes;D:\\mvn_resp\\org\\apache\\zookeeper\\zookeeper\\3.4.13\\zookeeper-3.4.13.jar;D:\\mvn_resp\\jline\\jline\\0.9.94\\jline-0.9.94.jar;D:\\mvn_resp\\org\\apache\\yetus\\audience-annotations\\0.5.0\\audience-annotations-0.5.0.jar;D:\\mvn_resp\\io\\netty\\netty\\3.10.6.Final\\netty-3.10.6.Final.jar;D:\\mvn_resp\\org\\apache\\curator\\curator-framework\\4.0.1\\curator-framework-4.0.1.jar;D:\\mvn_resp\\org\\apache\\curator\\curator-client\\4.0.1\\curator-client-4.0.1.jar;D:\\opensource\\github\\jmicro\\jmicro.api\\target\\classes;D:\\mvn_resp\\org\\slf4j\\slf4j-log4j12\\1.7.5\\slf4j-log4j12-1.7.5.jar;D:\\mvn_resp\\log4j\\log4j\\1.2.16\\log4j-1.2.16.jar;D:\\opensource\\github\\jmicro\\jmicro.common\\target\\classes;D:\\mvn_resp\\org\\javassist\\javassist\\3.24.0-GA\\javassist-3.24.0-GA.jar;D:\\mvn_resp\\org\\slf4j\\slf4j-api\\1.7.5\\slf4j-api-1.7.5.jar;D:\\mvn_resp\\com\\google\\code\\gson\\gson\\2.8.5\\gson-2.8.5.jar;D:\\opensource\\github\\jmicro\\jmicro.transport\\jmicro.transport.netty.server\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.server\\target\\classes;D:\\mvn_resp\\co\\paralleluniverse\\quasar-core\\0.7.10\\quasar-core-0.7.10.jar;D:\\mvn_resp\\io\\dropwizard\\metrics\\metrics-core\\3.2.3\\metrics-core-3.2.3.jar;D:\\mvn_resp\\org\\hdrhistogram\\HdrHistogram\\2.1.9\\HdrHistogram-2.1.9.jar;D:\\mvn_resp\\org\\latencyutils\\LatencyUtils\\2.0.3\\LatencyUtils-2.0.3.jar;D:\\mvn_resp\\com\\esotericsoftware\\kryo\\4.0.0\\kryo-4.0.0.jar;D:\\mvn_resp\\com\\esotericsoftware\\reflectasm\\1.11.3\\reflectasm-1.11.3.jar;D:\\mvn_resp\\com\\esotericsoftware\\minlog\\1.3.0\\minlog-1.3.0.jar;D:\\mvn_resp\\org\\objenesis\\objenesis\\2.2\\objenesis-2.2.jar;D:\\mvn_resp\\de\\javakaffee\\kryo-serializers\\0.42\\kryo-serializers-0.42.jar;D:\\mvn_resp\\io\\netty\\netty-all\\4.1.30.Final\\netty-all-4.1.30.Final.jar;D:\\opensource\\github\\jmicro\\jmicro.transport\\jmicro.transport.netty.client\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.client\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.registry\\jmicro.registry.zk\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.objfactory\\jmicro.objfactory.simple\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.config\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.limit\\target\\classes;D:\\mvn_resp\\com\\google\\guava\\guava\\20.0\\guava-20.0.jar;D:\\opensource\\github\\jmicro\\jmicro.idgenerator\\target\\classes;D:\\opensource\\github\\jmicro\\jmicro.redis\\target\\classes;D:\\mvn_resp\\redis\\clients\\jedis\\2.10.0\\jedis-2.10.0.jar;D:\\mvn_resp\\org\\apache\\commons\\commons-pool2\\2.4.3\\commons-pool2-2.4.3.jar",
				"org.jmicro.main.monitor.ServiceReqMonitor",new String[] {"test"});
		Assert.assertNotNull(result);
		
	}
	
	@Test
	public void testStartServiceByZK() {
		
		InstanceManager psm = of.get(InstanceManager.class);
		
		Assert.assertNotNull(psm);
	}
}
