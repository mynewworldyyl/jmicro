package cn.jmicro.choreography.ha;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;

@Component
@Service(namespace="ha",version="0.0.1",timeout=2000,retryCnt=0)
public class HaMonitorImpl implements IHaMonitor {

	private int id;
	
	@Cfg(value = "/HaMonitor/isMaster", defGlobal=false)
	private boolean isMaster = false;
	
	@Inject
	private ComponentIdServer idServer;
	
	public void ready() {
		this.id = idServer.getIntId(MasterStatus.class);
	}
	
	@Override
	public MasterStatus getStatus() {
		MasterStatus ms = new MasterStatus();
		ms.setId(id);
		return ms;
	}

}
