package org.jmicro.common;

import java.util.HashMap;
import java.util.Map;

import org.jmicro.common.config.Config;

public class JMicroContext {

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
	
	private Config cfg = null;
	
	private Map<String,Object> params = new HashMap<String,Object>();
	
	public Config getCfg() {
		return cfg;
	}
	
	public void getCfg(Config cfg) {
		this.cfg = cfg;
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
