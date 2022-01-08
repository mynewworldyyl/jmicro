package cn.jmicro.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;

public class ParamSupport {

	private final Set<FlowParamJRso> curCxt;
	
	public ParamSupport(Set<FlowParamJRso> cxt) {
		if(cxt == null) {
			this.curCxt = new HashSet<FlowParamJRso>();
		} else {
			this.curCxt = cxt;
		}
	}
	
	public Set<FlowParamJRso> cxt() {
		return curCxt;
	}
	
	public boolean exists(String key){
		return this.curCxt.contains(new FlowParamJRso(key));
	}
	
	public void mergeContext2ScriptEngine(ScriptEngine se) {
		if(curCxt != null && !curCxt.isEmpty()) {
			for(FlowParamJRso e : curCxt) {
				se.put(e.getName(), e.getVal());
			}
		}
	}
	
	public Map<String,Object> getInputMap(Map<String,Object> c) {
		if(c == null) {
			c = new HashMap<>();
		}else {
			c.clear();
		}
		
		if(curCxt != null && !curCxt.isEmpty()) {
			for(FlowParamJRso e : curCxt) {
				c.put(e.getName(), e.getVal());
			}
		}
		return c;
	}
	
	private FlowParamJRso get(String key) {
		if(key == null) key = "";
		for(FlowParamJRso e : curCxt) {
			if(key.equals(e.getName())) return e;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key, T defautl){
		FlowParamJRso fp = get(key);
		if(fp == null) return defautl;
		
		T v = (T)fp.getVal();
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	public void removeParam(String key){
	    this.curCxt.remove(new FlowParamJRso(key));
	}
	
	public <T> void setParam(String key,T val){
		FlowParamJRso p = get(key);
		if(p != null) {
			p.setVal(val);
			if(p.getClazz() == null && val != null) {
				p.setClazz(val.getClass().getName());
			}
		} else {
			p = new FlowParamJRso(key);
			p.setVal(val);
			if(val != null) {
				p.setClazz(val.getClass().getName());
			}
			this.curCxt.add(p);
		}
	}
	
	public void setInt(String key,int defautl){
	    this.setParam(key,defautl);
	}
	
	public void setByte(String key,byte defautl){
	    this.setParam(key,defautl);
	}
	
	public void setString(String key,String val){
		 this.setParam(key,val);
	}
	
	public void setBoolean(String key,boolean val){
		 this.setParam(key,val);
	}
	
	public void setFloat(String key,Float val){
		 this.setParam(key,val);
	}
	
	public void setDouble(String key,Double val){
		 this.setParam(key,val);
	}
	
	public void setLong(String key,Long val){
		 this.setParam(key,val);
	}
	
	public void setObject(String key,Object val){
		 this.setParam(key,val);
	}
	
	public Integer getInt(String key,Integer defautl){
		return this.getParam(key,defautl);
	}
	
	public Byte getByte(String key,Byte defautl){
		return this.getParam(key,defautl);
	}
	
	public Long getLong(String key,Long defautl){
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

	public void putAll(Map<String, Object> rst) {
		if(rst != null && !rst.isEmpty()) {
			rst.forEach((k,v)->{
				setParam(k,v);
			});
		}
	}
}
