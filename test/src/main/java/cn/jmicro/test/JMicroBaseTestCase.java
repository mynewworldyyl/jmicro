package cn.jmicro.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.security.ISecretService;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

@RunWith(BlockJUnit4ClassRunner.class)
public class JMicroBaseTestCase {

	public static final String TOPIC="/jmicro/test/topic01";
	
	protected static IObjectFactory of ;
	
	protected static IRegistry registry;
	
	@BeforeClass
	public static void setupTestClass() {
		of = JMicro.getObjectFactoryAndStart(getArgs());
		registry = of.get(IRegistry.class);
	}
	
	protected static String[] getArgs() {
		return new String[] {"-DinstanceName=comsumer","-DclientId=0","-DadminClientId=0","-DpriKeyPwd=comsumer"};
	}
	
	protected <T> T get(Class<T> cls) {
		return of.get(cls);
	}
	
	protected <T> T getSrv(Class<T> srvCls,String ns,String ver) {
		ServiceItem si = registry.getServices(srvCls.getName(),ns,ver)
				.iterator().next();
		org.junit.Assert.assertNotNull(si);
		T srv = of.getRemoteServie(si.getKey().getServiceName(), si.getKey().getNamespace()
				, si.getKey().getVersion(),null);
		return srv;
	}
	
	protected ServiceMethod helloTopicMethodKey() {
		StringBuilder sb = new StringBuilder();
		UniqueServiceKey.serviceName(sb, "cn.jmicro.example.api.ISayHello");
		UniqueServiceKey.namespace(sb, "simpleRpc");
		UniqueServiceKey.version(sb, "0.0.1");
		UniqueServiceKey.instanceName(true, sb, "provider");
		UniqueServiceKey.host(true, sb, Utils.getIns().getLocalIPList().get(0));
		UniqueServiceKey.port(false, sb, 0);
		sb.append("helloTopic").append(UniqueServiceMethodKey.SEP);
		sb.append("cn.jmicro.api.pubsub.PSData");
		UniqueServiceMethodKey key = UniqueServiceMethodKey.fromKey(sb.toString());
		ServiceMethod sm = new ServiceMethod();
		sm.setKey(key);
		return sm;
	}
	
	protected ServiceItem sayHelloServiceItem() {
		ServiceItem si = registry.getServices("cn.jmicro.example.api.rpc.ISimpleRpc","simpleRpc","0.0.1").iterator().next();
		org.junit.Assert.assertNotNull(si);
		return si;
	}
	
	protected ServiceMethod sayHelloServiceMethod() {
		ServiceItem si = sayHelloServiceItem();
		org.junit.Assert.assertNotNull(si);
		ServiceMethod sm = si.getMethod("hello", new String[] {"java.lang.String"});
		org.junit.Assert.assertNotNull(sm);
		return sm;
	}
	
	protected ServiceItem getServiceItem(String impl) {
		ServiceItem si = registry.getServiceByImpl(impl);
		org.junit.Assert.assertNotNull(si);
		return si;
	}
	
	protected ServiceMethod getServiceMethod(ServiceItem si,String methodName,Class<?>[] argTypes) {
		ServiceMethod sm = si.getMethod(methodName, argTypes);
		org.junit.Assert.assertNotNull(sm);
		return sm;
	}
	
	protected void setSayHelloContext() {
		
		JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
		//JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
		
		ServiceMethod sm = sayHelloServiceMethod();
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
		
		ServiceMethod sm = sayHelloServiceMethod();
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
