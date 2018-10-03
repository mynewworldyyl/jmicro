package org.jmicro.api;

import java.util.HashMap;
import java.util.Map;

public class JMicroContext  {


	public static String[] args = {};
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	public static final String SESSION_KEY="_sessionKey";	
	private static final ThreadLocal<JMicroContext> cxt = new ThreadLocal<JMicroContext>();
	
	private JMicroContext() {}
	
	public static JMicroContext get(){
		JMicroContext c = cxt.get();
		if(c == null) {
			c = new JMicroContext();
			cxt.set(c);
		}
		return c;
	}
	
	public static void remove(){
		JMicroContext c = cxt.get();
		if(c != null) {
			cxt.remove();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.params.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}

	public Integer getInt(String key,int defautl){
		return this.getParam(key,defautl);
	}
	
	public String getString(String key,String defautl){
		return this.getParam(key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return this.getParam(key,defautl);
	}
	
	
	public Float getFloat(String key,Float defautl){
		return this.getParam(key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return this.getParam(key,defautl);
	}
	
	public Object getObject(String key,Object defautl){
		return this.getParam(key,defautl);
	}
	
	
}
