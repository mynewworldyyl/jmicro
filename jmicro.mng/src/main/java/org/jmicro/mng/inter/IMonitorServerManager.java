package org.jmicro.mng.inter;

import org.jmicro.api.monitor.v2.MonitorServerStatus;

public interface IMonitorServerManager {

	public MonitorServerStatus[] status(boolean needTotal);
}
