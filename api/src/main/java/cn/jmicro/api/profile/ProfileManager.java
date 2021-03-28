package cn.jmicro.api.profile;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class ProfileManager {
	
	public static final String ROOT = Config.getRaftBasePath("") + "/profiles";

	private Map<String,Object> cacheValues = new HashMap<>();
	
	@Inject
	private IDataOperator op;
	
	public <T> T getVal(String module,String key, T defaultVal, Class<T> type) {
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai == null) {
			throw new CommonException("Not login to get user profile!");
		}
		return this.getVal(ai.getId(), module, key,defaultVal,type);
	}
	
	public <T> T getVal(Integer clientId,String module,String key, T defaultVal, Class<T> type) {
		return getFromZK(clientId, module,key,defaultVal,type);
	}
	
	public void setVal(Integer clientId,String module,String key,Object val) {
		String path = ROOT + "/" + clientId + "/" + module + "/" + key;
		
		KV kv = new KV();
		kv.setKey(key);
		kv.setVal(val);
		kv.setType(val.getClass().getName());
		
		String data = JsonUtils.getIns().toJson(kv);
		op.createNodeOrSetData(path, data, IDataOperator.PERSISTENT);
		if(this.cacheValues.containsKey(path)) {
			this.cacheValues.remove(path);
		}
	}

	private <T> T  getFromZK(Integer clientId, String module,String key, T defaultVal,Class<T> type) {
		String path = ROOT + "/" + clientId + "/" + module + "/" + key;
		if(this.cacheValues.containsKey(path)) {
			return (T)this.cacheValues.get(path);
		}
		if(op.exist(path)) {
			String data = op.getData(path);
			if(StringUtils.isNotEmpty(data)) {
				KV kv = JsonUtils.getIns().fromJson(data, KV.class);
				if(kv != null) {
					T v = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(kv.getVal()), type);
					if(v != null) {
						this.cacheValues.put(path, v);
						return v;
					}
				}
			}
		}else if(defaultVal != null){
			setVal(clientId, module, key, defaultVal);
		}
		return defaultVal;
	}
	
	
	
}
