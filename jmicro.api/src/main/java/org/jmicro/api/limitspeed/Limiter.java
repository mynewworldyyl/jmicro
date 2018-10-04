package org.jmicro.api.limitspeed;

import org.jmicro.api.server.IRequest;

public interface Limiter {

	public int apply(IRequest req);
}
