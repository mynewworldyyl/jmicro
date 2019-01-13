package org.jmicro.monitor.breaker.impl;

import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.annotation.Subscribe;
import org.jmicro.api.breaker.BreakerManager;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.registry.BreakRule;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.common.Constants;
import org.jmicro.monitor.breaker.api.IBreakerSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(namespace="org.jmicro.monitor.breaker.api.IBreakerSubscriber")
@Component
public class BreakerSubscriberImpl implements IBreakerSubscriber{

	private final static Logger logger = LoggerFactory.getLogger(BreakerSubscriberImpl.class);
	
	@Cfg(value="/BreakerSubscriberImpl/openDebug")
	private boolean openDebug = true;
	
	@Inject
	private DegradeManager degradeManager;
	
	@Inject
	private BreakerManager breakerManager;
	
	@Inject
	private ServiceManager serviceManager;
	
	@Override
	@Subscribe(topic=MonitorConstant.STATIS_SERVICE_METHOD_TOPIC)
	public void onStatics(PSData psData) {
		
		Map<Integer,Double> data = (Map<Integer,Double>)psData.getData();
		
		if(openDebug) {
			logger.debug("总请求:{}, 总响应:{}, TO:{}, TOF:{}, QPS:{}",
					data.get(MonitorConstant.CLIENT_REQ_BEGIN)
					,data.get(MonitorConstant.STATIS_TOTAL_RESP)
					,data.get(MonitorConstant.CLIENT_REQ_TIMEOUT)
					,data.get(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL)
					,data.get(MonitorConstant.STATIS_QPS)
					);
		}
		
		ServiceMethod sm = psData.get(Constants.SERVICE_METHOD_KEY);
		
		if(sm == null) {
			logger.error("Got NULL ServiceMethod topic :{}",psData.getTopic());
			return;
		}
		
		ServiceItem si = this.serviceManager.getServiceByServiceMethod(sm);
		if(si == null) {
			logger.error("Service not found:{}",sm.getKey().toKey(true, true, true));
			return;
		}
		
		sm = si.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
		
		if(sm == null) {
			logger.error("Got NULL ServiceMethod topic1 :{}",psData.getTopic());
			return;
		}
		
		if(!sm.getBreakingRule().isEnable()) {
			return;
		}
		
		BreakRule rule = sm.getBreakingRule();
		

		if(sm.isBreaking()) {
			//已经熔断,算成功率,判断是否关闭熔断器
			Double successPercent = data.get(MonitorConstant.STATIS_SUCCESS_PERCENT);
			if(successPercent > rule.getPercent()) {
				if(this.openDebug) {
					logger.debug("Close breaker for service {}, success rate {}",sm.toJson(),successPercent);
				}
				sm.setBreaking(false);
				breakerManager.breakService(sm);
			}
		} else {
			//没有熔断,判断是否需要熔断
			Double failPercent = data.get(MonitorConstant.STATIS_FAIL_PERCENT);
			if(failPercent > rule.getPercent()) {
				if(this.openDebug) {
					logger.debug("Break down service {}, fail rate {}",sm.toJson(),failPercent);
				}
				sm.setBreaking(true);
				breakerManager.breakService(sm);
			}
		}
	
	}
	
}
