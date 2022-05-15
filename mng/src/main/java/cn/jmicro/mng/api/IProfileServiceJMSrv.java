package cn.jmicro.mng.api;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.profile.KVJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IProfileServiceJMSrv {

	RespJRso<Map<String,Set<KVJRso>>> getKvs();
	
	RespJRso<Set<String>> getModuleList();
	
	RespJRso<Set<KVJRso>> getModuleKvs(String module);
	
	RespJRso<Boolean> updateKv(String module,KVJRso kv);
	
	IPromise<RespJRso<Boolean>> addKv(Integer scid, String module, String key, String valStr, Short type);
	
	IPromise<RespJRso<Boolean>> addModule(Integer scid, String module);
}
