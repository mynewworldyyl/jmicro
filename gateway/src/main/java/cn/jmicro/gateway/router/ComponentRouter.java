package cn.jmicro.gateway.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.gateway.GatewayConstant;
import cn.jmicro.api.gateway.IRoute;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.gateway.lb.MessageRouteTable;

@Component(value="apigatewayRouter")
public class ComponentRouter implements IRoute {

	@Inject
	private MessageRouteTable mrt;
	
	@Cfg("/gateway/level")
	private int level = 1;
	
	@Inject
	private Map<String,IRoute> selectors = new HashMap<>();
	
	public void jready() {
		//把自己从列表中删除
		selectors.remove("apigatewayRouter");
	}
	
	@Override
	public List<MessageRouteRow> doRoute(ISession s, Message msg) {
		List<MessageRouteRow> rows = null; 
		if(level == GatewayConstant.LEVEL_FIRST) {
			rows = route2SecondGateway(s,msg);
		} 
		
		if(rows == null) {
			//二级业务网关 或一级网关直转服务实例
			msg.setOuterMessage(false);
			return route2Service(s,msg);
		}
		
		return null;
	}

	private List<MessageRouteRow> route2SecondGateway(ISession s, Message msg) {
		return null;
	}

	private List<MessageRouteRow> route2Service(ISession s, Message msg) {
		//rpc服务方法路由
		Set<Integer> instanceIds = mrt.getIntanceIdsByMethodCode(msg.getSmKeyCode());
		if(instanceIds == null || instanceIds.isEmpty()) return null;
		List<MessageRouteRow> l = new ArrayList<>();
		for(Integer iid : instanceIds) {
			MessageRouteRow r = mrt.getRow(iid);
			if(r != null) {
				l.add(r);
			}
		}
		return l;
	}

}
