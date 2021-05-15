package cn.jmicro.gateway;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IBaseGatewayService;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.util.HashUtils;

@Service(external=true,version="0.0.1",logLevel=MC.LOG_NO,showFront=false)
@Component
public class BaseGatewayServiceImpl implements IBaseGatewayService {

	 private Logger logger = LoggerFactory.getLogger(BaseGatewayServiceImpl.class);
	 
	@Inject
	private ApiGatewayHostManager hostManager;
	
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
