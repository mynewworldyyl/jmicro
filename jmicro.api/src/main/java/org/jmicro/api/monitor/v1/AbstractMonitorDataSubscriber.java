package org.jmicro.api.monitor.v1;

import org.jmicro.api.net.ISession;
import org.jmicro.common.CommonException;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	public static final Short[] YTPES = ISession.STATIS_TYPES;
	
	@Override
	public Double getData(String srvKey,Short type) {
		throw new CommonException("getData not support for type: "+type);
	}

}
