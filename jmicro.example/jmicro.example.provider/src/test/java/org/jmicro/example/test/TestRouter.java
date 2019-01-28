package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.route.IRouter;
import org.jmicro.api.route.RouteEndpoint;
import org.jmicro.api.route.RouteRule;
import org.jmicro.api.route.RuleManager;
import org.junit.Test;

public class TestRouter {

	@Test
	public void testAddRule() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=TestRouter -Dclient=true"});
		RuleManager ruleManager = of.get(RuleManager.class);
		
		//ip rule
		RouteRule rr = new RouteRule();
		rr.setEnable(true);
		rr.setId("TestIpRoute");
		rr.setPriority(1000);
		rr.setType(IRouter.TYPE_IP_TO_IP);
		
		
		RouteEndpoint from = new RouteEndpoint();
		from.setIpPort("192.168.1.102");
		//from.setIpPort("172.16.22.7");
		rr.setFrom(from);
		
		RouteEndpoint to = new RouteEndpoint();
		to.setIpPort("192.168.1.102");
		//to.setIpPort("172.16.22.7");
		rr.setTo(to);
		
		ruleManager.addOrUpdate(rr);
		
		
		//service rule
		rr = new RouteRule();
		rr.setEnable(true);
		rr.setId("TestServiceRoute");
		rr.setPriority(1001);
		rr.setType(IRouter.TYPE_CLIENT_SERVICE_MATCH);
		
		from = new RouteEndpoint();
		from.setMethod("getPerson");
		from.setNamespace("testrpc");
		from.setServiceName("org.jmicro.example.api。ITestRpcService");
		from.setVersion("0.0.1");
		rr.setFrom(from);
		
		to = new RouteEndpoint();
		to.setIpPort("192.168.1.102");
		//to.setIpPort("172.16.22.7");
		rr.setTo(to);
		
		ruleManager.addOrUpdate(rr);
		
		
		//tag rule
		rr = new RouteRule();
		rr.setEnable(true);
		rr.setId("TestTagRoute");
		rr.setPriority(1003);
		rr.setType(IRouter.TYPE_CONTEXT_PARAMS_MATCH);
		
		
		from = new RouteEndpoint();
		from.setTagKey("routerTag");
		from.setTagVal("tagValue");
		rr.setFrom(from);
		
		to = new RouteEndpoint();
		//to.setIpPort("192.168.1.102");
		to.setIpPort("172.16.22.7");
		rr.setTo(to);
		
		ruleManager.addOrUpdate(rr);
	}
}