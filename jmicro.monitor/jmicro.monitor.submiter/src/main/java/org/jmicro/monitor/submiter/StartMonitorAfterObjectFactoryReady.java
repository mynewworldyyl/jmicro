package org.jmicro.monitor.submiter;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IFactoryListener;

@Component(active=true,value="startMonitorAfterObjectFactoryReady")
public class StartMonitorAfterObjectFactoryReady implements IFactoryListener {

	@Override
	public void afterInit(IObjectFactory of) {
		SubmitItemHolderManager sihm = of.get(SubmitItemHolderManager.class);
		sihm.startWork("");
	}

	@Override
	public int runLevel() {
		return 1000;
	}
	
	@Override
	public void preInit(IObjectFactory of) {
	}
}
