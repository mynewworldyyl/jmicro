package cn.jmicro.example.test;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.route.IRouter;
import cn.jmicro.api.route.RouteEndpoint;
import cn.jmicro.api.route.RouteRule;
import cn.jmicro.api.route.RuleManager;

public class TestRouter {

	@Test
	public void testAddRule() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=TestRouter -Dclient=true"});
		RuleManager ruleManager = of.get(RuleManager.class);
		
		//ip rule
		RouteRule rr = new RouteRule();
		rr.setEnable(true);
		rr.setUniqueId("TestIpRoute");
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
		rr.setUniqueId("TestServiceRoute");
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
		rr.setUniqueId("TestTagRoute");
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
