package cn.jmicro.api.gateway;

import cn.jmicro.api.annotation.IDStrategy;
import lombok.Data;

@Data
@IDStrategy
public class MessageRouteRow {
	
	public static final byte TYPE_SERVICE = 1;
	
	public static final byte TYPE_GATEWAY = 2;
	
	public static final String MSG_ROUTE_KEYS="/msgRoute/keys";
	
	public static final String API_MODEL="/msgRoute/work";
	
	public static final boolean API_MODEL_PRE = true;//前置模式，可以转发给worker，也可以直接转发给服务
	
	public static final boolean API_MODEL_WORKER = false;//工作者模式，或业务网关模式，直接面对服务实例

	private Integer id;
	
	private String key;
	
	private String ip;
	
	private String port;
	
	private int backendType = TYPE_SERVICE;
	
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
