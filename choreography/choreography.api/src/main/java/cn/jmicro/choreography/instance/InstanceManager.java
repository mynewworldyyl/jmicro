package cn.jmicro.choreography.instance;

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
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.choreography.api.IInstanceListener;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class InstanceManager {

	private final static Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	
	@Inject
	private IDataOperator op;
	
	private Map<String,ProcessInfo> sysProcesses = new HashMap<>();
	
	private String agentId = null;
	
	private Map<String,ProcessInfo> mngProcesses = new HashMap<>();
	
	private Set<IInstanceListener> listeners = new HashSet<>();
	
	private Map<String,Long> timeouts = new HashMap<>();
	private Object notifyObj = new Object();
	
	private long rmTimeout = 10000;
	
	private boolean mngSysProcess = true;
	
	private IDataListener insDataListener = (path,data)->{
		if(StringUtils.isEmpty(data)) {
			logger.warn("Data is NULL for path: " + path);
			return;
		}
		
		String id = path.substring(ChoyConstants.INS_ROOT.length()+1);
		ProcessInfo pi = JsonUtils.getIns().fromJson(data,ProcessInfo.class);
		
		if(this.mngSysProcess && StringUtils.isEmpty(pi.getAgentId())) {
			this.sysProcesses.put(id, pi);
			return;
		}
		
		if(StringUtils.isNotEmpty(pi.getAgentId())) {
			mngProcesses.put(id, pi);
			notifyListener(IListener.DATA_CHANGE,pi);
		}
	};
	
	public void init() {
		op.addChildrenListener(ChoyConstants.INS_ROOT, (type,p,c,data)->{
			if(type == IListener.ADD) {
				instanceAdded(c,data);
			} else if(type == IListener.REMOVE) {
				synchronized(notifyObj) {
					//等待rmTimeout毫秒后，如果结点还是不存在，删除正式删除实例
					timeouts.put(c, System.currentTimeMillis());
					notifyObj.notify();
				}
			}
		});
		
		new Thread(this::check).start();
		
	}
	
	public void check() {
		while(true) {
			synchronized(notifyObj) {
				if(timeouts.isEmpty()) {
					try {
						notifyObj.wait(rmTimeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if(timeouts.isEmpty()) {
					continue;
				}
				
				long curTime = System.currentTimeMillis();
				Set<String> set = new HashSet<>();
				set.addAll(timeouts.keySet());
				
				for(String to : set) {
					if(curTime - timeouts.get(to) > rmTimeout) {
						String p = ChoyConstants.INS_ROOT +"/" + to;
						if(!op.exist(p)) {
							//结点删除
							instanceRemoved(to);
							timeouts.remove(to);
						}
					}
				}
			
			}
		}
		
	}
	
	public void filterByAgent(String agentId) {
		mngSysProcess = false;
		if(!sysProcesses.isEmpty()) {
			sysProcesses.clear();
		}
		this.agentId = agentId;
		if(!mngProcesses.isEmpty()) {
			Set<ProcessInfo> pis = new HashSet<>();
			pis.addAll(mngProcesses.values());
			for(ProcessInfo pi : pis) {
				if(!agentId.equals(pi.getAgentId())) {
					mngProcesses.remove(pi.getId());
				}
			}
		}
	}

	private void instanceRemoved(String c) {
		logger.debug("Instance remove ID: " + c);
		
		if(mngProcesses.containsKey(c)) {
			ProcessInfo pi = mngProcesses.remove(c);
			notifyListener(IListener.REMOVE,pi);
			String p = ChoyConstants.INS_ROOT +"/" + pi.getId();
			op.removeDataListener(p, this.insDataListener);
		} else {
			if(mngSysProcess && sysProcesses.containsKey(c)) {
				sysProcesses.remove(c);
				String p = ChoyConstants.INS_ROOT +"/" + c;
				op.removeDataListener(p, this.insDataListener);
			} 
		}
	}

	private void notifyListener(int type, ProcessInfo pi) {
		if(this.listeners.isEmpty()) {
			return;
		}
		
		for(IInstanceListener l : this.listeners) {
			l.instance(type, pi);
		}
	}

	private void instanceAdded(String c, String data) {
		
		ProcessInfo pi = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
		if(pi == null) {
			return;
		}
		
		//logger.debug("Instance Add: " + data);
		
		if(this.agentId != null && !this.agentId.equals(pi.getAgentId())) {
			//对于Agent端，只需要管理Agent相关的进程实例
			return;
		}
		
		if(this.mngSysProcess && StringUtils.isEmpty(pi.getAgentId())) {
			//非Agent进程
			String p = ChoyConstants.INS_ROOT +"/" + pi.getId();
			op.addDataListener(p, this.insDataListener);
			this.sysProcesses.put(c, pi);
			return;
		}
		
		if(StringUtils.isNotEmpty(pi.getAgentId())) {
			String p = ChoyConstants.INS_ROOT +"/" + pi.getId();
			op.addDataListener(p, this.insDataListener);
			mngProcesses.put(c, pi);
			notifyListener(IListener.ADD,pi);
		}
	}
	
	public Set<ProcessInfo> getProcesses(boolean all) {
		Set<ProcessInfo> inses = new HashSet<>();
		inses.addAll(this.mngProcesses.values());
		if(all) {
			if(mngSysProcess) {
				inses.addAll(this.sysProcesses.values());
			}else {
				logger.warn("This instanceManager is not a system process mamager to cannot show all process in system!");
			}
		} 
		return inses;
	}
	
	
	public Set<ProcessInfo> getProcessesByAgentId(String agentId) {
		if(StringUtils.isEmpty(agentId)) {
			return null;
		}
		Set<ProcessInfo> pis = new HashSet<>();
		for(ProcessInfo pi : mngProcesses.values()) {
			if(agentId.equals(pi.getAgentId())) {
				pis.add(pi);
			}
		}
		return pis;
	}
	
	public int getProcessSizeByDepId(String depid) {
		if(StringUtils.isEmpty(depid)) {
			return 0;
		}
		int cnt = 0;
		for(ProcessInfo pi : mngProcesses.values()) {
			if(depid.equals(pi.getDepId())) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public int getProcessSizeByAgentId(String agentId) {
		if(StringUtils.isEmpty(agentId)) {
			return 0;
		}
		int cnt = 0;
		for(ProcessInfo pi : mngProcesses.values()) {
			if(agentId.equals(pi.getAgentId())) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public Set<ProcessInfo> getProcessesByDepId(String depid) {
		if(StringUtils.isEmpty(depid)) {
			return null;
		}
		Set<ProcessInfo> pis = new HashSet<>();
		for(ProcessInfo pi : mngProcesses.values()) {
			if(depid.equals(pi.getDepId())) {
				pis.add(pi);
			}
		}
		return pis;
	}
	
	public ProcessInfo getProcessesByInsId(String insId,boolean includeSys) {
		if(mngProcesses.containsKey(insId)) {
			return mngProcesses.get(insId);
		}else if(includeSys) {
			return sysProcesses.get(insId);
		}
		return null;
	}
	
	public ProcessInfo getProcessesByAgentIdAndDepid(String agentId,String depId) {
		for(ProcessInfo pi : mngProcesses.values()) {
			if(depId.equals(pi.getDepId()) && agentId.equals(pi.getAgentId())) {
				return pi;
			}
		}
		return null;
	}
	
	public boolean isExistByAgentId(String agentId) {
		if(mngProcesses.isEmpty()) {
			return false;
		}
		for(ProcessInfo pi : mngProcesses.values()) {
			if(agentId.equals(pi.getAgentId())) {
				return true;
			}
		}
		return false;
	}
	
	public void addListener(IInstanceListener l) {
		if(!listeners.contains(l)) {
			listeners.add(l);
		}
		if(mngProcesses.isEmpty()) {
			return ;
		}
		for(ProcessInfo pi : mngProcesses.values()) {
			l.instance(IListener.ADD, pi);
		}
		
	}
	
	public void removeListener(IInstanceListener l) {
		if(listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
}
