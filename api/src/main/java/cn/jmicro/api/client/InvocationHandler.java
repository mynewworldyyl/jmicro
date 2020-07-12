package cn.jmicro.api.client;

public interface InvocationHandler {

	 public <T> T invoke(Object proxy, String method, Object[] args);
	 
}
