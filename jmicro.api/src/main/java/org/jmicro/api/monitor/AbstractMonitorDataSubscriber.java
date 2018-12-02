package org.jmicro.api.monitor;

import org.jmicro.common.CommonException;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	@Override
	public Double getData(String srvKey,Integer type) {
		throw new CommonException("getData not support for type: "+type);
	}

}
