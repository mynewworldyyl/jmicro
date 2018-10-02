package org.jmicro.api.server;

import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;

import net.techgy.idgenerator.IDStrategy;

@IDStrategy
public interface IRequest extends IEncodable,IDecodable {

	public String getServiceName();

	//public void setServiceName(String serviceName);
	//public String getImpl();

	//public void setImpl(String impl);

	public String getNamespace();

	//public void setGroup(String group);

	public String getVersion();

	//public void setVersion(String version);
	
	public String getMethod();
	//public void setMethod(String method);

	public Object[] getArgs();
	//public void setArgs(Object[] args);
	
	public Long getRequestId();
	
	public ISession getSession();
}
