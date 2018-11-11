package org.jmicro.api.gateway;

import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.net.Message;

public final class ApiRequest {
	
	private Map<String,Object> params = new HashMap<>();
	
	private String serviceName = "";
	
	private String method = "";
	
	private Object[] args = null;
	
	private String namespace = "";
	
	private String version = "";
	
	private Long reqId = -1L;
	
	private transient Message msg = null;
	
	public Map<String, Object> getParams() {
		return params;
	}
	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public Long getReqId() {
		return reqId;
	}
	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
	
}
