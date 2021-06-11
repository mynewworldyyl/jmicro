package cn.jmicro.api.gateway;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;

@Component
public class MessageRoutTable {
	
	public static final String TABLE_ROOT = Config.getRaftBasePath("/apiroute");

	private final Map<String,MessageRouteGroup> gtables = new HashMap<>();
	
	private final Map<String,MessageRouteGroup> stables = new HashMap<>();
	
	private RaftNodeDataListener<MessageRouteRow> configListener;
	
	@Inject
	private IDataOperator op;
	
	private IRaftListener<MessageRouteRow> lis = new IRaftListener<MessageRouteRow>() {
		public void onEvent(int type,String id, MessageRouteRow rr) {
			if(type == IListener.DATA_CHANGE) {
				routeConfigChanged(id,rr);
			}else if(type == IListener.REMOVE){
				routeConfigRemove(rr,true);
			}else if(type == IListener.ADD) {
				routeConfigAdd(rr);
			}
		}
	};
	
	public void ready() {
		configListener = new RaftNodeDataListener<>(op,TABLE_ROOT,MessageRouteRow.class,false);
		configListener.addListener(lis);
	}
	
	public MessageRouteGroup getGatewayRouteGroup(String key) {
		return gtables.get(key);
	}
	
	public MessageRouteGroup getServiceRouteGroup(String key) {
		return stables.get(key);
	}
	
	protected void routeConfigAdd(MessageRouteRow rr) {
		MessageRouteGroup g = this.getList(rr.getKey(), rr.getBackendType());
		if(g == null) {
			if(rr.getBackendType() == MessageRouteRow.TYPE_GATEWAY) {
				synchronized(gtables) {
					gtables.put(rr.getKey(), g = new MessageRouteGroup());
				}
			}else {
				synchronized(stables) {
					stables.put(rr.getKey(), g= new MessageRouteGroup());
				}
			}
			g.setKey(rr.getKey());
		}
		g.addRoute(rr);
	}

	private MessageRouteGroup getList(String key, int backendType) {
		MessageRouteGroup g = null;
		if(backendType == MessageRouteRow.TYPE_GATEWAY) {
			g = gtables.get(key);
			if(g == null) {
				gtables.put(key, g = new MessageRouteGroup());
			}
		}else{
			g = stables.get(key);
			if(g == null) {
				stables.put(key, g = new MessageRouteGroup());
			}
		}
		return g;
	}

	protected void routeConfigRemove(MessageRouteRow rr,boolean removeLock) {
		MessageRouteGroup g = getList(rr.getKey(),rr.getBackendType());
		if(g != null) {
			g.removeRoute(rr.getId());
		};
	}

	private void routeConfigChanged(String cid, MessageRouteRow rr) {
		MessageRouteGroup g = getList(rr.getKey(),rr.getBackendType());
		if(g != null) {
			g.updateRoute(rr);
		};
	}
}
