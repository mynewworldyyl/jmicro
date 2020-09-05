package cn.jmicro.mng.api;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.api.profile.KV;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IProfileService {

	Resp<Map<String,Set<KV>>> getKvs();
	
	Resp<Set<String>> getModuleList();
	
	Resp<Set<KV>> getModuleKvs(String module);
	
	Resp<Boolean> updateKv(String module,KV kv);
	
}
