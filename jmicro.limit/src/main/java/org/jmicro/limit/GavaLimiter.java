package org.jmicro.limit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.common.Constants;

import com.google.common.util.concurrent.RateLimiter;

@Component(value="gavaLimiter")
public class GavaLimiter  implements ILimiter{

	private Map<String,RateLimiter> rateLimiter = new HashMap<>();
	
	@Override
	public boolean apply(IRequest req) {
		String key = this.key(req);
		
		ServiceMethod sm = (ServiceMethod)JMicroContext.get()
				.getObject(Constants.SERVICE_METHOD_KEY, null);
		
		ServiceItem item = (ServiceItem)JMicroContext.get()
				.getObject(Constants.SERVICE_ITEM_KEY, null);
		
		int timeout = sm.getTimeout();
		RateLimiter rl = rateLimiter.get(key);
		if(rl == null) {
			int maxSpeed = -1;
			if(sm != null){
				maxSpeed = sm.getMaxSpeed();
			}
			
			if(maxSpeed == -1 && item != null){
				maxSpeed = item.getMaxSpeed();
			}
			
			if(maxSpeed > 0) {
				rl = RateLimiter.create(sm.getMaxSpeed());
				this.rateLimiter.put(key, rl);
			}else {
				//不限速
				return true;
			}
		}
		
		if(timeout < 0){
			timeout = item.getTimeout();
		}
		
		if(timeout > 0){
			return rl.tryAcquire(1, timeout, TimeUnit.MILLISECONDS);
		}else {
			rl.acquire(1);
		}
		
		return true;
	}
	
	private String key(IRequest req){
		String key = ServiceMethod.methodParamsKey(req.getArgs());
		key = key + ServiceItem.serviceName(req.getServiceName(), req.getNamespace(), req.getVersion());
		key = key + req.getMethod();
		return key;
	}

}
