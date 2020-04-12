package org.jmicro.mng.inter;

import org.jmicro.api.pubsub.PubsubServerStatus;

public interface IPubsubServerManager {

	public PubsubServerStatus[] status(boolean needTotal);
}
