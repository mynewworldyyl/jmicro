package cn.jmicro.mng.impl;

import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.mng.api.ICommonManager;

@Component
@Service(namespace="mng", version="0.0.1")
public class CommonManagerImpl implements ICommonManager {

	@Cfg("/notLonginClientId")
	private int notLonginClientId = 10;
	
	@Inject
	private I18NManager i18nManager;
	
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
	
}
