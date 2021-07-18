package cn.jmicro.gateway;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IBaseGatewayServiceJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.common.util.HashUtils;

@Service(external=true,version="0.0.1",logLevel=MC.LOG_NO,showFront=false)
@Component
public class BaseGatewayServiceImpl implements IBaseGatewayServiceJMSrv {

	 private Logger logger = LoggerFactory.getLogger(BaseGatewayServiceImpl.class);
	 
	@Inject
	private ApiGatewayHostManager hostManager;
	
	@Inject
	private IRegistry reg;
	
	/*public void ready() {
		reg.addServiceNameListener(IBaseGatewayServiceJMSrv.class.getName(), (type,siKey,item)->{
			if(type == IListener.ADD) {
				ServiceMethodJRso sm = item.getMethod("fnvHash1a");
				logger.info("fnvHash1a key: "+sm.getKey().fullStringKey());
				logger.info("fnvHash1a code: "+sm.getKey().getSnvHash());
			}
		});
	}*/
	
	@Override
	@SMethod(needLogin=false)
	public List<String> getHosts(String protocal) {
		return hostManager.getHosts(protocal);
	}

	@Override
	@SMethod(needLogin=false)
	public String bestHost(String protocal) {
		return hostManager.bestHost(protocal);
	}

	@Override
	@SMethod(needLogin=false)
	public int fnvHash1a(String methodKey) {
		//int code = HashUtils.FNVHash1(methodKey);
		//logger.info("fnvHash1a: " + code + " => " + methodKey);
		return HashUtils.FNVHash1(methodKey);
	}
	
}
