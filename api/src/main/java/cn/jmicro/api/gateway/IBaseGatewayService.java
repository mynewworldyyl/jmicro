package cn.jmicro.api.gateway;

import java.util.List;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IBaseGatewayService {

	List<String> getHosts(String protocal);
	
	String bestHost(String protocal);
	
	int fnvHash1a(String str);
}
