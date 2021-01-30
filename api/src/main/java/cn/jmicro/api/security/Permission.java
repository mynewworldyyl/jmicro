package cn.jmicro.api.security;

import cn.jmicro.api.annotation.SO;

@SO
public class Permission {
	
	public static final String ACT_INVOKE = "Invoke";
	
	public static final String ACT_ADD = "Add";
	public static final String ACT_UPDATE = "Update";
	public static final String ACT_DELETE = "Delete";
	public static final String ACT_QUERY = "Query";
	
	//public static final String NAME_INVOKE = "invoke";
	private int haCode;
	
	//private String pid;
	
	private String label;
	
	private String desc;
	
	private String modelName;
	
	private String actType;
	
	//比如某个服务方法的KEY
	//private String key;

	public String getLabel() {
		return label;
	}

	public int getHaCode() {
		return haCode;
	}

	public void setHaCode(int haCode) {
		this.haCode = haCode;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDesc() {
		return desc;
	}
	
	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getActType() {
		return actType;
	}

	public void setActType(String actType) {
		this.actType = actType;
	}
		
}
