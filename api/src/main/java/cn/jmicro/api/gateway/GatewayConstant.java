package cn.jmicro.api.gateway;

public interface GatewayConstant {

	public static final byte LEVEL_FIRST = 1;
	
	public static final byte LEVEL_SECOND = 2;
	
	//public static final String MSG_ROUTE_KEYS = "/msgRoute/keys";
	
	public static final String API_MODEL="/msgRoute/work";
	
	public static final boolean API_MODEL_PRE = true;//前置模式，可以转发给worker，也可以直接转发给服务
	
	public static final boolean API_MODEL_WORKER = false;//工作者模式，或业务网关模式，直接面对服务实例
	

}
