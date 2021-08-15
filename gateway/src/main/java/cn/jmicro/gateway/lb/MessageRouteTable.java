package cn.jmicro.gateway.lb;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.gateway.MessageRouteGroup;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.gateway.ApiGatewayPostFactory;

@Component
public class MessageRouteTable {
	
	private final static Logger logger = LoggerFactory.getLogger(MessageRouteTable.class);
	
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
	
	public void jready() {
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
		
		srvMng.addListener((type,siKey,sit)->{
			if(sit == null) {
				sit = this.srvMng.getServiceByKey(siKey.fullStringKey());
				if(sit == null) {
					logger.warn("Service item not found: " + siKey.fullStringKey());
					return;
				}
			}
			if(!sit.isExternal()) {
				return;
			}
			if(type == IListener.ADD) {
				serviceAdd(sit);
			}else if(type == IListener.REMOVE) {
				serviceRemove(sit);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(sit);
			}
		});
		
	}

	private void serviceDataChange(ServiceItemJRso si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.updateServiceItem(si);
		removeMethodCode2Instance(si);
		parseMethodCode2Instance(si);
	}

	private void serviceRemove(ServiceItemJRso si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.removeServiceItem(si);
		removeMethodCode2Instance(si);
	}

	private void removeMethodCode2Instance(ServiceItemJRso si) {

		Set<ServiceMethodJRso> methods = si.getMethods();
		if(methods == null || methods.isEmpty()) return;
		
		for(ServiceMethodJRso sm : methods) {
			Set<Integer> l = method2Instances.get(sm.getKey().getSnvHash());
			if(l != null) {
				l.remove(si.getInsId());
			}
		}
	}

	private void serviceAdd(ServiceItemJRso si) {
		MessageRouteGroup mrg = instance2Services.get(si.getInsId());
		if(mrg == null) return;
		mrg.addServiceItem(si);
		parseMethodCode2Instance(si);
	}

	private void parseMethodCode2Instance(ServiceItemJRso si) {
		Set<ServiceMethodJRso> methods = si.getMethods();
		if(methods == null || methods.isEmpty()) return;
		
		for(ServiceMethodJRso sm : methods) {
			Integer h = sm.getKey().getSnvHash();
			Set<Integer> l = method2Instances.get(h);
			if(l == null) {
				method2Instances.put(h, l = new HashSet<>());
			}
			
			if(logger.isDebugEnabled()) {
				logger.debug("Method route hash:{},key:{},insId: {} insName:{}",h,sm.getKey().methodID(),si.getInsId(),sm.getKey().getUsk().getInstanceName());
			}
			
			l.add(si.getInsId());
		}
	}

	private void processDataChange(ProcessInfoJRso pi) {
		MessageRouteGroup mrg = instance2Services.get(pi.getId());
		if(mrg == null) {
			mrg = new MessageRouteGroup();
			mrg.setPi(pi);
			instance2Services.put(pi.getId(), mrg);
		}
		//removeMessageType2Instance(pi);
		parseMessageType2Instance(pi);
		routeRowChange(pi);
	}

	private void routeRowChange(ProcessInfoJRso pi) {
		addRouteRow(pi);
	}

	private void processRemove(ProcessInfoJRso pi) {
		instance2Services.remove(pi.getId());
		removeMessageType2Instance(pi);
		tables.remove(pi.getId());
	}

	private void removeMessageType2Instance(ProcessInfoJRso pi) {
		Set<Byte> types = pi.getTypes();
		if(types == null || types.isEmpty()) return;
		for(Byte t : types) {
			Set<Integer> l = messageType2Instances.get(t);
			if(l != null) {
				l.remove(pi.getId());
			}
		}
	}

	private void processAdd(ProcessInfoJRso pi) {
		processDataChange(pi);
		addRouteRow(pi);
	}
	
	private void addRouteRow(ProcessInfoJRso pi) {
		MessageRouteRow mrr = tables.get(pi.getId());
		if(mrr == null) {
			 mrr = new MessageRouteRow();
		}
		mrr.setInsId(pi.getId());
		mrr.setInsName(pi.getInstanceName());
		mrr.setIp(pi.getHost());
		mrr.setPort(pi.getPort());
		//mrr.setSessionKey(sessionKey);
		tables.put(pi.getId(), mrr);
	}

	private void parseMessageType2Instance(ProcessInfoJRso pi) {
		Set<Byte> types = pi.getTypes();
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
		tables.put(rr.getInsId(), rr);
	}

	protected void routeConfigRemove(MessageRouteRow rr,boolean removeLock) {
		tables.remove(rr.getInsId());
	}

	private void routeConfigChanged(String cid, MessageRouteRow rr) {
		tables.put(rr.getInsId(), rr);
	}

	public Set<Integer> getIntanceIdsByMessageType(Byte type) {
		return this.messageType2Instances.get(type);
	}

	public Set<Integer> getIntanceIdsByMethodCode(Integer smKeyCode) {
		Set<Integer> ins = this.method2Instances.get(smKeyCode);
		if(ins != null) {
			return ins;
		}
		ServiceMethodJRso sm = srvMng.getServiceMethodByHash(smKeyCode);
		
		
		return this.method2Instances.get(smKeyCode);
	}

	public MessageRouteRow getRow(Integer iid) {
		return tables.get(iid);
	}
}
