package cn.jmicro.resource;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.monitor.ResourceMonitorConfig;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.RaftNodeDataListener;

@Component
public class ResourceMonitorConfigManager {

	private final static Logger logger = LoggerFactory.getLogger(ResourceMonitorConfigManager.class);
	
	private RaftNodeDataListener<ResourceMonitorConfig> instanceListener = null;
	
	@Inject
	private IDataOperator op;
	
	public void ready() {
		instanceListener = new RaftNodeDataListener<>(op,ChoyConstants.INS_ROOT,ResourceMonitorConfig.class,true);
		instanceListener.addListener((type,node,pi)->{
			notifyConfigListener(type,pi);
		});
	}
	
	private void notifyConfigListener(int type, ResourceMonitorConfig cfg) {
		if(type == IListener.ADD) {
			
		}else if(type == IListener.DATA_CHANGE) {
			
		}
	}

	public void forEach(Consumer<ResourceMonitorConfig> c) {
		instanceListener.forEachNode(c);
	}
	
	public ResourceMonitorConfig getConfigById(Integer pid) {
		ResourceMonitorConfig pi = this.instanceListener.getData(pid+"");
		return pi;
	}

}
