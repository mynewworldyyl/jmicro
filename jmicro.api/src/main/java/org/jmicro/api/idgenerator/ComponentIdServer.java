package org.jmicro.api.idgenerator;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

@Component
public class ComponentIdServer implements IIdClient,IIdServer{

	@Inject(value="idClient",required = false)
	private IIdClient idGenerator;
	
	@Inject(value = Constants.DEFAULT_IDGENERATOR, required = false)
	private IIdServer idServer;
	
	private boolean isNotIdServer = false;

	public void init() {
		if(idGenerator == null && idServer == null) {
			throw new CommonException("IIdClient and IIdServer is NULL");
		}
		isNotIdServer = idServer == null;
	}
	
	@Override
	public String[] getStringIds(String idKey, int num) {
		if(isNotIdServer) {
			return idGenerator.getStringIds(idKey, num);
		}else {
			return idServer.getStringIds(idKey, num);
		}
	}

	@Override
	public Long[] getLongIds(String idKey, int num) {
		if(isNotIdServer) {
			return idGenerator.getLongIds(idKey, num);
		}else {
			return idServer.getLongIds(idKey, num);
		}
	}

	@Override
	public Integer[] getIntIds(String idKey, int num) {
		if(isNotIdServer) {
			return idGenerator.getIntIds(idKey, num);
		}else {
			return idServer.getIntIds(idKey, num);
		}
	}

	@Override
	public Long getLongId(String idKey) {
		if(isNotIdServer) {
			return idGenerator.getLongId(idKey);
		}else {
			return idServer.getLongId(idKey);
		}
	}

	@Override
	public String getStringId(String idKey) {
		if(isNotIdServer) {
			return idGenerator.getStringId(idKey);
		}else {
			return idServer.getStringId(idKey);
		}
	}

	@Override
	public Integer getIntId(String idKey) {
		if(isNotIdServer) {
			return idGenerator.getIntId(idKey);
		}else {
			return idServer.getIntId(idKey);
		}
	}
	
}
