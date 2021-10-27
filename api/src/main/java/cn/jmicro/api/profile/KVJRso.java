package cn.jmicro.api.profile;

import cn.jmicro.api.annotation.SO;

@SO
public  class KVJRso {

	private Object val;
	private String type;
	private String key;

	public Object getVal() {
		return val;
	}

	public void setVal(Object val) {
		this.val = val;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	
}
