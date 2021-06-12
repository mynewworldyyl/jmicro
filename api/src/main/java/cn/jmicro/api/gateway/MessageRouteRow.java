package cn.jmicro.api.gateway;

import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.annotation.IDStrategy;
import lombok.Data;

@Data
@IDStrategy
public class MessageRouteRow {

	private Integer id;
	
	private String key;
	
	private String ip;
	
	private String port;
	
	private String insName;
	
	private int insId;
	
	private Set<Integer> methodCodes = new HashSet<>();
	
	//private int backendType = GatewayConstant.TYPE_SERVICE;
	
	private transient String sessionKey;

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof MessageRouteRow)) return false;
		MessageRouteRow rr = (MessageRouteRow)obj;
		return this.hashCode() == rr.hashCode();
	}

	@Override
	public int hashCode() {
		/*if(key == null) {
			key = ip+":" + port;
		}*/
		return key.hashCode();
	}
	
	
}
