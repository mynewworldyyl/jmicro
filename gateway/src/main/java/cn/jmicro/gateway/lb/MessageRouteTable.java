package cn.jmicro.gateway.lb;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.gateway.MessageRouteGroup;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.gateway.ApiGatewayPostFactory;

@Component
public class MessageRouteTable {
	
	private final Map<Integer,MessageRouteGroup> instance2Services = new ConcurrentHashMap<>();
	
	private final Map<Integer,MessageRouteRow> tables = new ConcurrentHashMap<>();
	
	//数据包类型对应运行实例
	private final Map<Byte,Set<Integer>> messageType2Instances = new ConcurrentHashMap<>();
	
	private final Map<Integer,Set<Integer>> method2Instances = new ConcurrentHashMap<>();
	
	private RaftNodeDataListener<MessageRouteRow> configListener;
	
	@Inject
	private ProcessInstanceManager insMng;
	
	@Inject
	private ServiceManager srvMng;
	
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
		configListener = new RaftNodeDataListener<>(op,ApiGatewayPostFactory.TABLE_ROOT,
				MessageRouteRow.class,false);
		configListener.addListener(lis);
		
		insMng.addInstanceListner((type,pi)->{
			if(type == IListener.ADD) {
				processAdd(pi);
			}else if(type == IListener.REMOVE) {
				processRemove(pi);
			}else if(type == IListener.DATA_CHANGE) {
				processDataChange(pi);
			}
		});
		
		srvMng.addListener((type,si)->{
			if(type == IListener.ADD) {
				serviceAdd(si);
			}else if(type == IListener.REMOVE) {
				serviceRemove(si);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(si);
			}
		});
		
	}

	private void serviceDataChange(ServiceItem si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.updateServiceItem(si);
		removeMethodCode2Instance(si);
		parseMethodCode2Instance(si);
	}

	private void serviceRemove(ServiceItem si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.removeServiceItem(si);
		removeMethodCode2Instance(si);
	}

	private void removeMethodCode2Instance(ServiceItem si) {

		Set<ServiceMethod> methods = si.getMethods();
		if(methods == null || methods.isEmpty()) return;
		
		for(ServiceMethod sm : methods) {
			Set<Integer> l = method2Instances.get(sm.getKey().getSnvHash());
			if(l != null) {
				l.remove(si.getInsId());
			}
		}
	}

	private void serviceAdd(ServiceItem si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.addServiceItem(si);
		parseMethodCode2Instance(si);
	}

	private void parseMethodCode2Instance(ServiceItem si) {
		Set<ServiceMethod> methods = si.getMethods();
		if(methods == null || methods.isEmpty()) return;
		
		for(ServiceMethod sm : methods) {
			Set<Integer> l = method2Instances.get(sm.getKey().getSnvHash());
			if(l == null) {
				method2Instances.put(sm.getKey().getSnvHash(), l = new HashSet<>());
			}
			l.add(si.getInsId());
		}
	}

	private void processDataChange(ProcessInfo pi) {
		MessageRouteGroup mrg = instance2Services.get(pi.getId());
		if(mrg == null) {
			mrg = new MessageRouteGroup();
			mrg.setPi(pi);
			instance2Services.put(pi.getId(), mrg);
		}
		//removeMessageType2Instance(pi);
		parseMessageType2Instance(pi);
	}

	private void processRemove(ProcessInfo pi) {
		instance2Services.remove(pi.getId());
		removeMessageType2Instance(pi);
	}

	private void removeMessageType2Instance(ProcessInfo pi) {
		List<Byte> types = pi.getTypes();
		if(types == null || types.isEmpty()) return;
		for(Byte t : types) {
			Set<Integer> l = messageType2Instances.get(t);
			if(l != null) {
				l.remove(pi.getId());
			}
		}
	}

	private void processAdd(ProcessInfo pi) {
		processDataChange(pi);
	}
	
	private void parseMessageType2Instance(ProcessInfo pi) {
		List<Byte> types = pi.getTypes();
		if(types == null || types.isEmpty()) return;
		for(Byte t : types) {
			Set<Integer> l = messageType2Instances.get(t);
			if(l == null) {
				messageType2Instances.put(t, l = new HashSet<>());
			}
			l.add(pi.getId());
		}
	}
	
	public MessageRouteRow getGatewayRouteGroup(Integer key) {
		return tables.get(key);
	}
	
	protected void routeConfigAdd(MessageRouteRow rr) {
		tables.put(rr.getId(), rr);
	}

	protected void routeConfigRemove(MessageRouteRow rr,boolean removeLock) {
		tables.remove(rr.getId());
	}

	private void routeConfigChanged(String cid, MessageRouteRow rr) {
		tables.put(rr.getId(), rr);
	}

	public Set<Integer> getIntanceIdsByMessageType(Byte type) {
		return this.messageType2Instances.get(type);
	}

	public Set<Integer> getIntanceIdsByMethodCode(Integer smKeyCode) {
		return this.method2Instances.get(smKeyCode);
	}

	public MessageRouteRow getRow(Integer iid) {
		return tables.get(iid);
	}
}
