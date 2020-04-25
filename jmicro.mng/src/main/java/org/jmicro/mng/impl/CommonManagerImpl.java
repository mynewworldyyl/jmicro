package org.jmicro.mng.impl;

import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.i18n.I18NManager;
import org.jmicro.mng.inter.ICommonManager;

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
