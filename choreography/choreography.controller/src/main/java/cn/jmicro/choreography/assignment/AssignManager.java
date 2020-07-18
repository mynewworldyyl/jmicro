package cn.jmicro.choreography.assignment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.choreography.agent.AgentManager;
import cn.jmicro.choreography.assign.Assign;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class AssignManager {

	private final static Logger logger = LoggerFactory.getLogger(AssignManager.class);
	
	private Map<String,Set<Assign>> agent2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	private Map<String,Assign> ins2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	private Map<String,Set<Assign>> dep2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private IDataOperator op;
	
	private IDataListener assignDataListener = (path,data)->{
		Assign a = this.getAssignByJson(data);
		if(a == null) {
			logger.error("Data listener assign is null for: " +path + ", data: " + data);
			return;
		}
		addOrUpdate(a);
	};
	
	private IChildrenListener childListener = (type,p,c,data)->{
		if(type == IListener.ADD) {
			Assign a = getAssignByJson(data);
			if(a == null) {
				logger.error("Child listener assign is null for: " +p + ", data: " + data);
				return;
			}
			addOrUpdate(a);
			op.addDataListener(p+"/"+c, assignDataListener);
		} else if(type == IListener.REMOVE) {
			op.removeDataListener(p+"/"+c, assignDataListener);
		}
	};
	
	public void ready() {
		this.agentManager.addAgentListener((int type,String agentId, AgentInfo agent)->{
			String apath = ChoyConstants.ROOT_AGENT + "/" + agentId;
			if(type == IListener.ADD) {
				op.addChildrenListener(apath, childListener);
			}else if(type == IListener.REMOVE) {
				op.removeChildrenListener(apath, childListener);
			}
		});
	}
	
	private void addOrUpdate(Assign a) {
		if(!this.agent2Assigns.containsKey(a.getAgentId())) {
			synchronized(this) {
				if(!this.agent2Assigns.containsKey(a.getAgentId())) {
					this.agent2Assigns.put(a.getAgentId(), new HashSet<Assign>());
				}
			}
		}
		
		if(!this.dep2Assigns.containsKey(a.getDepId())) {
			synchronized(this) {
				if(!this.dep2Assigns.containsKey(a.getDepId())) {
					this.dep2Assigns.put(a.getDepId(), new HashSet<Assign>());
				}
			}
		}
		
		this.ins2Assigns.put(a.getInsId(),a);
		this.agent2Assigns.get(a.getAgentId()).add(a);
		this.dep2Assigns.get(a.getDepId()).add(a);
	}

	private Assign getAssignByJson(String data) {
		if(StringUtils.isEmpty(data)) {
			logger.warn("Data is NULL for path: " + data);
			return null;
		}
		
		Assign a = JsonUtils.getIns().fromJson(data, Assign.class);
		if(a == null) {
			logger.error("Agent  data: " + data);
			return null;
		}
		return a;
	}

	public Set<Assign> getAll() {
		Set<Assign> all = new HashSet<>();
		all.addAll(ins2Assigns.values());
		return all;
	}
	
	public void remove(Assign a) {
		String path = assignPath(a);
		if(op.exist(path)) {
			op.deleteNode(path);
		} else {
			removeLocalCache(a);
		}
	}
	
	private void removeLocalCache(Assign a) {
		this.ins2Assigns.remove(a.getInsId());
		this.agent2Assigns.get(a.getAgentId()).remove(a);
		this.dep2Assigns.get(a.getDepId()).remove(a);
	}

	public void update(Assign a) {
		String path = assignPath(a);
		if(op.exist(path)) {
			op.setData(path, JsonUtils.getIns().toJson(a));
		} else {
			removeLocalCache(a);
			logger.error("Not exist assing when update: " + a.toString());
		}
	}
	
	public void add(Assign a) {
		String path = assignPath(a);
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(a), IDataOperator.PERSISTENT);
	}
	
	private String assignPath(Assign a) {
		return ChoyConstants.ROOT_AGENT + "/" + a.getAgentId() + "/" + a.getInsId();
	}
	
	public Assign getAssignByInfoId(String id) {
		return this.ins2Assigns.get(id);
	}
	
	@SuppressWarnings({"unchecked" })
	public Set<Assign> getAssignByDepId(String depId) {
		if(StringUtils.isEmpty(depId) || !this.dep2Assigns.containsKey(depId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		s.addAll(this.dep2Assigns.get(depId));
		return s;
	}
	
	
	@SuppressWarnings({ "unchecked" })
	public Set<Assign> getAssignByAgentId(String agentId) {
		if(StringUtils.isEmpty(agentId) || !this.agent2Assigns.containsKey(agentId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		s.addAll(this.agent2Assigns.get(agentId));
		return s;
	}
	
	@SuppressWarnings({ "unchecked" })
	public Set<Assign> getAssignByDepIdAndAgentId(String depId, String agentId) {
		if(StringUtils.isEmpty(agentId) || StringUtils.isEmpty(depId) ) {
			return Collections.EMPTY_SET;
		}
		
		Set<Assign> as = getAssignByAgentId(agentId);
		if(as.isEmpty()) {
			return as;
		}
		
		Set<Assign> ds = getAssignByDepId(agentId);
		if(ds.isEmpty()) {
			return ds;
		}
		
		as.retainAll(ds);
		
		return as;
	}
	
}
