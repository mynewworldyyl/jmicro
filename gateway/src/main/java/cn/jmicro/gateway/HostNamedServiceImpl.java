package cn.jmicro.gateway;

import java.util.List;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IHostNamedService;

@Service(external=true,namespace="gateway",version="0.0.1")
@Component
public class HostNamedServiceImpl implements IHostNamedService {

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

}
