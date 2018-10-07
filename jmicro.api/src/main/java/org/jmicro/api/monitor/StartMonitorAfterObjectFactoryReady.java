package org.jmicro.api.monitor;

import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IPostFactoryReady;

@PostListener(true)
public class StartMonitorAfterObjectFactoryReady implements IPostFactoryReady {

	@Override
	public void ready(IObjectFactory of) {
		SubmitItemHolderManager sihm = of.get(SubmitItemHolderManager.class);
		sihm.startWork();
	}

}
