package cn.jmicro.api.gateway;

import java.util.List;

import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;

public interface ISelector {

	MessageRouteRow select(List<MessageRouteRow> mrrs,ISession session, Message msg);
	
}
