package org.jmicro.monitor.submiter;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IPostFactoryReady;

@Component(active=true,value="startMonitorAfterObjectFactoryReady")
public class StartMonitorAfterObjectFactoryReady implements IPostFactoryReady {

	@Override
	public void ready(IObjectFactory of) {
		SubmitItemHolderManager sihm = of.get(SubmitItemHolderManager.class);
		sihm.startWork("");
	}
	
}
