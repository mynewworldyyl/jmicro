package cn.jmicro.choreography.agent;

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
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.choreography.assign.Assign;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class AssignManager {

	private final static Logger logger = LoggerFactory.getLogger(AssignManager.class);
	
	//private Map<String,Set<Assign>> agent2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	private Map<Integer,Assign> ins2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	private Map<String,Set<Assign>> dep2Assigns = Collections.synchronizedMap(new HashMap<>());
	
	@Inject
	private IDataOperator op;
	
	private String agentId = null;
	
	private IAssignListener assignListener;
	
	private IDataListener assignDataListener = (path,data)->{
		Assign a = this.getAssignByJson(data);
		if(a == null) {
			String msg = "Data listener assign is null for: " +path + ", data: " + data;
			LG.log(MC.LOG_ERROR, AssignManager.class, msg);
			logger.error(msg);
			return;
		}
		addOrUpdate(a);
		assignListener.change(IListener.DATA_CHANGE, a);
	};
	
	private IChildrenListener childListener = (type,p,c,data)->{
		if(type == IListener.ADD) {
			Assign a = getAssignByJson(data);
			if(a == null) {
				String msg = "Child listener assign is null for: " +p + ", data: " + data;
				LG.log(MC.LOG_ERROR, AssignManager.class, msg);
				logger.error(msg);
				return;
			}
			addOrUpdate(a);
			op.addDataListener(p+"/"+c, assignDataListener);
			if(assignListener != null) {
				assignListener.change(IListener.ADD, a);
			}
		} else if(type == IListener.REMOVE) {
			op.removeDataListener(p+"/"+c, assignDataListener);
			if(assignListener != null) {
				assignListener.change(IListener.REMOVE, ins2Assigns.get(Integer.parseInt(c)));
			}
			Assign a = ins2Assigns.remove(Integer.parseInt(c));
			dep2Assigns.get(a.getDepId()).remove(a);
		}
	};
	
	public void ready() {
		//this.agentManager.addAgentListener(agentListener);
	}
	
	public void doInit(IAssignListener assignListener, String agentId) {
		this.agentId = agentId;
		this.assignListener = assignListener;

		if(StringUtils.isEmpty(agentId)) {
			return;
		}
		
		this.agentId = agentId;
		String path = ChoyConstants.ROOT_AGENT + "/" + agentId;
		op.addChildrenListener(path, childListener);
		
	}
	
	private void addOrUpdate(Assign a) {
		if(!this.dep2Assigns.containsKey(a.getDepId())) {
			synchronized(this) {
				if(!this.dep2Assigns.containsKey(a.getDepId())) {
					this.dep2Assigns.put(a.getDepId(), new HashSet<Assign>());
				}
			}
		}
		
		this.ins2Assigns.put(a.getInsId(),a);
		//this.agent2Assigns.get(a.getAgentId()).add(a);
		this.dep2Assigns.get(a.getDepId()).add(a);
	}

	private Assign getAssignByJson(String data) {
		if(StringUtils.isEmpty(data)) {
			String msg = "Data is NULL for path: " + data;
			LG.log(MC.LOG_WARN, AssignManager.class, msg);
			logger.warn(msg);
			return null;
		}
		
		Assign a = JsonUtils.getIns().fromJson(data, Assign.class);
		if(a == null) {
			String msg = "Agent  data: " + data;
			LG.log(MC.LOG_ERROR, AssignManager.class, msg);
			logger.error(msg);
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
		this.dep2Assigns.get(a.getDepId()).remove(a);
	}

	public void update(Assign a) {
		String path = assignPath(a);
		if(op.exist(path)) {
			op.setData(path, JsonUtils.getIns().toJson(a));
		} else {
			removeLocalCache(a);
			String msg = "Not exist assing when update: " + a.toString();
			LG.log(MC.LOG_ERROR, AssignManager.class, msg);
			logger.error(msg);
		}
	}
	
	public void add(Assign a) {
		String path = assignPath(a);
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(a), IDataOperator.PERSISTENT);
	}
	
	private String assignPath(Assign a) {
		return ChoyConstants.ROOT_AGENT + "/" + a.getAgentId() + "/" + a.getInsId();
	}
	
	public Assign getAssignByInfoId(Integer id) {
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

}
