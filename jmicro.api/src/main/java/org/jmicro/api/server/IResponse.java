package org.jmicro.api.server;

import org.jmicro.api.IDable;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;

public interface IResponse extends IEncodable,IDecodable,IDable{

	Long getRequestId();
	
	Object getResult();
}
