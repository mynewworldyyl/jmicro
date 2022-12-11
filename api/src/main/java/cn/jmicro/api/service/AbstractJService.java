package cn.jmicro.api.service;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.security.ActInfoJRso;

public abstract class AbstractJService {

	protected ActInfoJRso getAct() {
		return JMicroContext.get().getAccount();
	}
	
	protected Integer getActId() {
		return getAct().getId();
	}
	
	protected int clientId() {
		return getAct().getClientId();
	}
	
}
