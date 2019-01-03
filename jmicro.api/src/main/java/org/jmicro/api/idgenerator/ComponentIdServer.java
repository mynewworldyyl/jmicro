package org.jmicro.api.idgenerator;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

@Component
public class ComponentIdServer /*implements IIdClient,IIdServer*/{

	/**
	 * 非ID服务本身获取ID
	 */
	@Inject(value="idClient",required = false)
	private IdClient idClient;
	
	/**
	 * id服务本身要获取ID，直接通过IIdServer服务本向获取
	 */
	@Inject(value = Constants.DEFAULT_IDGENERATOR, required = false)
	private IIdServer idServer;
	
	private boolean isNotIdServer = false;

	public void init() {
		if(idClient == null && idServer == null) {
			throw new CommonException("IIdClient and IIdServer is NULL");
		}
		isNotIdServer = idServer == null;
	}
	
	public String[] getStringIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getStringIds(idKey, num);
		}else {
			return idServer.getStringIds(idKey, num);
		}
	}

	public Long[] getLongIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getLongIds(idKey, num);
		}else {
			return idServer.getLongIds(idKey, num);
		}
	}

	public Integer[] getIntIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getIntIds(idKey, num);
		}else {
			return idServer.getIntIds(idKey, num);
		}
	}

	public Long getLongId(String idKey) {
		if(isNotIdServer) {
			return idClient.getLongId(idKey);
		}else {
			return idServer.getLongId(idKey);
		}
	}

	public String getStringId(String idKey) {
		if(isNotIdServer) {
			return idClient.getStringId(idKey);
		}else {
			return idServer.getStringId(idKey);
		}
	}

	public Integer getIntId(String idKey) {
		if(isNotIdServer) {
			return idClient.getIntId(idKey);
		} else {
			return idServer.getIntId(idKey);
		}
	}
	
}
