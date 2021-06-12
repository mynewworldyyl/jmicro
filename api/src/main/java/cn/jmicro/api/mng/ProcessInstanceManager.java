package cn.jmicro.api.mng;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.common.Utils;

@Component(limit2Packages={"cn.jmicro"},level=20)
public class ProcessInstanceManager {

	private final static Logger logger = LoggerFactory.getLogger(ProcessInstanceManager.class);
	
	private RaftNodeDataListener<ProcessInfo> instanceListener = null;
	
	private Set<IInstanceListener> insListeners = new HashSet<>();
	
	@Cfg(value="/enable")
	private boolean enable = false;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ProcessInfo pi;
	
	public void ready() {
		instanceListener = new RaftNodeDataListener<>(op,ChoyConstants.INS_ROOT,ProcessInfo.class,true);
		instanceListener.addListener((type,node,pi)->{
			notifyListener(type,pi);
		});
	}
	
	public void forEach(Consumer<ProcessInfo> c) {
		instanceListener.forEachNode(c);
	}
	
	public void forEachProcessInfoName(Consumer<String> c) {
		instanceListener.forEachNodeName(c);
	}
	
	public ProcessInfo getInstanceById(Integer pid) {
		ProcessInfo pi = this.instanceListener.getData(pid+"");
		return pi;
	}
	
	public ProcessInfo getProcessByName(String insName) {
		if(Utils.isEmpty(insName)) {
			throw new NullPointerException();
		}
		ProcessInfo[] id = new ProcessInfo[1];
		this.instanceListener.forEachNode((node)->{
			if(insName.equals(node.getInstanceName())) {
				id[0] = node;
			}
		});
		return id[0];
	}
	
	public Set<ProcessInfo> getProcessByNamePreifx(String insNamePrefix) {
		if(Utils.isEmpty(insNamePrefix)) {
			throw new NullPointerException();
		}
		Set<ProcessInfo> pis = new HashSet<>();
		this.instanceListener.forEachNode((node)->{
			if(node.getInstanceName().startsWith(insNamePrefix)) {
				pis.add(node);
			}
		});
		return pis;
	}
	
	public boolean isMonitorable(Integer pid) {
		ProcessInfo pi = getInstanceById(pid);
		if(pi != null) {
			return pi.isMonitorable();
		}else {
			return false;
		}
	}
	
	public boolean isMonitorable(String insName) {
		ProcessInfo pi = getProcessByName(insName);
		if(pi != null) {
			return pi.isMonitorable();
		}else {
			return false;
		}
	}
	
	public void addInstanceListner(IInstanceListener l) {
		if(!this.insListeners.contains(l)) {
			instanceListener.forEachNode((pi)->{
				l.onEvent(IListener.ADD, pi);
			});
			insListeners.add(l);
		}
	}
	
	public void removeInstanceListner(IInstanceListener l) {
		if(this.insListeners.contains(l)) {
			insListeners.remove(l);
		}
	}
	
	public void notifyListener(int type, ProcessInfo pi) {
		if(!this.insListeners.isEmpty()) {
			for(IInstanceListener l : this.insListeners) {
				l.onEvent(type, pi);
			}
		}
	}
	
	public static interface IInstanceListener extends IListener{
		void onEvent(int type,ProcessInfo pi);
	}
	
}
