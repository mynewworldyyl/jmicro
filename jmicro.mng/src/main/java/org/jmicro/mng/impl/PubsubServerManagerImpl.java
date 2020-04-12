package org.jmicro.mng.impl;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.pubsub.PubsubServerStatus;
import org.jmicro.mng.inter.IPubsubServerManager;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年3月27日
 */
@Component
@Service(namespace="mng",version="0.0.1")
public class PubsubServerManagerImpl implements IPubsubServerManager {

	@Override
	public PubsubServerStatus[] status(boolean needTotal) {
		
		return null;
	}
	
	
}
