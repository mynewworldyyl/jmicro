package cn.jmicro.gateway;

import java.util.List;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IBaseGatewayService;
import cn.jmicro.common.util.HashUtils;

@Service(external=true,namespace="gateway",version="0.0.1")
@Component
public class BaseGatewayServiceImpl implements IBaseGatewayService {

	@Inject
	private ApiGatewayHostManager hostManager;
	
	@Override
	public List<String> getHosts(String name) {
		return hostManager.getHosts();
	}

	@Override
	public String bestHost() {
		return hostManager.bestHost();
	}

	@Override
	public int fnvHash1a(String methodKey) {
		return HashUtils.FNVHash1(methodKey);
	}
	
}
