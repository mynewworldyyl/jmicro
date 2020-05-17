package cn.jmicro.mng.impl;

import java.util.Map;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.mng.api.ICommonManager;

@Component
@Service(namespace="mng", version="0.0.1")
public class CommonManagerImpl implements ICommonManager {

	@Inject
	private I18NManager i18nManager;
	
	@Override
	public Map<String, String> getI18NValues(String lang) {
		return i18nManager.values(lang);
	}
	
}
