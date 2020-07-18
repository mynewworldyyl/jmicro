package cn.jmicro.api.gateway;

import java.util.List;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IHostNamedService {

	List<String> getHosts(String name);
	
	String bestHost();
}
