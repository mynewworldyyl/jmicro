package org.jmicro.api.monitor;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

public interface IMonitorDataSubmiter {

	void submit(int type,IRequest req, IResponse resp,Object... args);
}
