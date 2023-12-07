package cn.jmicro.api.gateway;

import java.util.List;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.profile.KVJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IBaseGatewayServiceJMSrv {

	IPromise<RespJRso<Boolean>> hearbeat();
	
	List<String> getHosts(String protocal);
	
	String bestHost(String protocal);
	
	int fnvHash1a(String str);
	
	IPromise<RespJRso<Set<KVJRso>>> getSCidModuleKvs(Integer scid,String module);
	
	Integer timeMsSync();
	
	IPromise<RespJRso<Integer>> timeMsAsync();
}
