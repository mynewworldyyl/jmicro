package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.i18n.II18N;
import cn.jmicro.api.monitor.MC;

@Component
public class TypeII18NImpl implements II18N {
	
	@Override
	public String key() {
		return "en";
	}

	@Override
	public Map<String, String> values() {
		Map<String,String> data = new HashMap<>();
		for(Short t : MC.MONITOR_VAL_2_KEY.keySet()) {
			data.put("statis.index."+t, MC.MONITOR_VAL_2_KEY.get(t));
		}
		return data;
	}

	
}
