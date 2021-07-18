package cn.jmicro.test;

import org.junit.Before;
import org.junit.runner.RunWith;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

@RunWith(JMicroJUnitTestRunner.class)
public class JMicroBaseTestCase {

	public static final String TOPIC="/jmicro/test/topic01";
	
	protected static IObjectFactory of ;
	
	protected static IRegistry registry;
	
	protected static ServiceManager srvMng;
	
	@Before
	public void setupTestClass() {
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		of = (IObjectFactory)JMicro.getObjectFactoryAndStart(getArgs());
		//of = EnterMain.getObjectFactoryAndStart(getArgs());
		registry = of.get(IRegistry.class);
		srvMng = of.get(ServiceManager.class, false);
	}
	
	//-DinstanceName=ServiceComsumer -DclientId=0 -DadminClientId=0  
	//-Dlog4j.configuration=../../log4j.xml -DsysLogLevel=1 -Dpwd=0
	protected String[] getArgs() {
		return new String[] {"-DinstanceName=JMicroBaseTestCase","-DclientId=0","-DadminClientId=0","-DpriKeyPwd=comsumer"
		,"-DsysLogLevel=1","-Dlog4j.configuration=../../log4j.xml","-Dpwd=0","-DsysLogLevel=1"};
	}
	
	protected <T> T get(Class<T> cls) {
		return of.get(cls);
	}
	
	protected <T> T getSrv(Class<T> srvCls,String ns,String ver) {
		UniqueServiceKeyJRso si = registry.getServices(srvCls.getName(),ns,ver)
				.iterator().next();
		org.junit.Assert.assertNotNull(si);
		T srv = of.getRemoteServie(si.getServiceName(), si.getNamespace()
				, si.getVersion(),null);
		return srv;
	}
	
	protected ServiceMethodJRso helloTopicMethodKey() {
		StringBuilder sb = new StringBuilder();
		UniqueServiceKeyJRso.serviceName(sb, "cn.expjmicro.example.api.rpc.ISayHello");
		UniqueServiceKeyJRso.namespace(sb, "simpleRpc");
		UniqueServiceKeyJRso.version(sb, "0.0.1");
		UniqueServiceKeyJRso.instanceName(true, sb, "provider");
		UniqueServiceKeyJRso.host(true, sb, Utils.getIns().getLocalIPList().get(0));
		UniqueServiceKeyJRso.port(false, sb, "0");
		sb.append("helloTopic").append(UniqueServiceMethodKeyJRso.SEP);
		sb.append("cn.jmicro.api.pubsub.PSData");
		UniqueServiceMethodKeyJRso key = UniqueServiceMethodKeyJRso.fromKey(sb.toString());
		ServiceMethodJRso sm = new ServiceMethodJRso();
		sm.setKey(key);
		return sm;
	}
	
	protected ServiceItemJRso sayHelloServiceItem() {
		UniqueServiceKeyJRso si = registry.getServices("cn.expjmicro.example.api.rpc.ISimpleRpc","exampleProvider","0.0.1").iterator().next();
		org.junit.Assert.assertNotNull(si);
		return srvMng.getServiceByKey(si.fullStringKey());
	}
	
	protected ServiceMethodJRso sayHelloServiceMethod() {
		ServiceItemJRso siKey = sayHelloServiceItem();
		org.junit.Assert.assertNotNull(siKey);
		ServiceMethodJRso sm = siKey.getMethod("hello");
		org.junit.Assert.assertNotNull(sm);
		return sm;
	}
	
	protected UniqueServiceKeyJRso getServiceItem(int hash) {
		UniqueServiceKeyJRso si = registry.getServiceByCode(hash);
		org.junit.Assert.assertNotNull(si);
		return si;
	}
	
	protected ServiceMethodJRso getServiceMethod(ServiceItemJRso si,String methodName,Class<?>[] argTypes) {
		ServiceMethodJRso sm = si.getMethod(methodName, argTypes);
		org.junit.Assert.assertNotNull(sm);
		return sm;
	}
	
	protected void setSayHelloContext() {
		
		JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
		//JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
		
		ServiceMethodJRso sm = sayHelloServiceMethod();
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY,sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY,sayHelloServiceItem());
		JMicroContext.get().setString(JMicroContext.CLIENT_SERVICE, sm.getKey().getServiceName());
		JMicroContext.get().setString(JMicroContext.CLIENT_NAMESPACE, sm.getKey().getNamespace());
		JMicroContext.get().setString(JMicroContext.CLIENT_VERSION, sm.getKey().getVersion());
		JMicroContext.get().setString(JMicroContext.CLIENT_METHOD, sm.getKey().getMethod());
	}
	
	protected void setSayHelloContextv2() {
		
		JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, true);
		StatisMonitorClient monitor = of.get(StatisMonitorClient.class);
		
		ServiceMethodJRso sm = sayHelloServiceMethod();
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY,sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY,sayHelloServiceItem());
		JMicroContext.get().setString(JMicroContext.CLIENT_SERVICE, sm.getKey().getServiceName());
		JMicroContext.get().setString(JMicroContext.CLIENT_NAMESPACE, sm.getKey().getNamespace());
		JMicroContext.get().setString(JMicroContext.CLIENT_VERSION, sm.getKey().getVersion());
		JMicroContext.get().setString(JMicroContext.CLIENT_METHOD, sm.getKey().getMethod());
	}
	
	protected void waitForReady(float wainTimeInSeconds) {
		//System.out.println("**************Sleep "+wainTimeInSeconds+"S to wait for subscribe service to ready");
		try {
			Thread.sleep((long)(1000*wainTimeInSeconds));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
