package cn.jmicro.gateway;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IBaseGatewayService;
import cn.jmicro.common.util.HashUtils;

@Service(external=true,version="0.0.1")
@Component
public class BaseGatewayServiceImpl implements IBaseGatewayService {

	 private Logger logger = LoggerFactory.getLogger(BaseGatewayServiceImpl.class);
	 
	@Inject
	private ApiGatewayHostManager hostManager;
	
	@Override
	@SMethod(needLogin=false)
	public List<String> getHosts(String name) {
		return hostManager.getHosts();
	}

	@Override
	@SMethod(needLogin=false)
	public String bestHost() {
		return hostManager.bestHost();
	}

	@Override
	@SMethod(needLogin=false)
	public int fnvHash1a(String methodKey) {
		//int code = HashUtils.FNVHash1(methodKey);
		//logger.info("fnvHash1a: " + code + " => " + methodKey);
		return HashUtils.FNVHash1(methodKey);
	}
	
}
