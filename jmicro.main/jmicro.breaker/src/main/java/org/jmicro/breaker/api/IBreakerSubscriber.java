package org.jmicro.breaker.api;

import org.jmicro.api.annotation.Service;
import org.jmicro.api.pubsub.PSData;

@Service(namespace="org.jmicro.monitor.breaker.api.IBreakerSubscriber")
public interface IBreakerSubscriber {

	public void onStatics(PSData psData);
	
}
