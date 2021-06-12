package cn.jmicro.gateway.lb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.gateway.ISelector;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;

@Component(value="defBalance")
public class ComponentSelector implements ISelector {

	@Inject
	private Map<String,ISelector> selectors = new HashMap<>();
	
	private Random r = new Random();
	
	public void ready() {
		selectors.remove("defBalance");
	}
	
	@Override
	public MessageRouteRow select(List<MessageRouteRow> mrrs, ISession session, Message msg) {
		if(mrrs == null || mrrs.isEmpty()) return null;
		int s = mrrs.size();
		if(s == 1) return mrrs.get(0);
		return mrrs.get(r.nextInt()%s);
	}

}
