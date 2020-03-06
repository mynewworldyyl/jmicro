package org.jmicro.monitor.submiter;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.PostFactoryAdapter;

@Component(active=true,value="startMonitorAfterObjectFactoryReady")
public class StartMonitorAfterObjectFactoryReady extends PostFactoryAdapter {

	@Override
	public void afterInit(IObjectFactory of) {
		SubmitItemHolderManager sihm = of.get(SubmitItemHolderManager.class);
		sihm.init0();
	}
}
