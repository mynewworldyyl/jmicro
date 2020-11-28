package cn.jmicro.api.gateway;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IBaseGatewayService {

	List<String> getHosts(String name);
	
	String bestHost();
	
	int fnvHash1a(String str);
}
