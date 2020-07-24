package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.DictManager;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.api.mng.ICommonManager;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace="mng", version="0.0.1",external=true,debugMode=1,showFront=false)
public class CommonManagerImpl implements ICommonManager {

	@Cfg("/notLonginClientId")
	private int notLonginClientId = 10;
	
	@Inject
	private I18NManager i18nManager;
	
	@Inject
	private DictManager dictManager;
	
	@Override
	public Map<String, String> getI18NValues(String lang) {
		return i18nManager.values(lang);
	}

	@Override
	public boolean hasPermission(int per) {
		if(JMicroContext.get().hasPermission(per)) {
			return true;
		} else {
			return notLoginPermission(per);
		}
	}
	
	@Override
	public boolean notLoginPermission(int per) {
		return per >= this.notLonginClientId;
	}

	@Override
	public Resp<Map<String,Object>> getDicts(String[] keys) {
		Map<String,Object> dicts = new HashMap<>();
		for(String k : keys) {
			if(StringUtils.isNotEmpty(k)) {
				dicts.put(k, this.dictManager.getDict(k));
			}
		}
		
		Resp<Map<String,Object>> resp = new Resp<>();
		resp.setData(dicts);
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}
	
	
	
}
