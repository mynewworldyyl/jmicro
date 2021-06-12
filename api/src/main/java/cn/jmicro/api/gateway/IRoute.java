package cn.jmicro.api.gateway;

import java.util.List;

import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;

public interface IRoute {

	List<MessageRouteRow> doRoute(ISession s,Message msg);
	
}
