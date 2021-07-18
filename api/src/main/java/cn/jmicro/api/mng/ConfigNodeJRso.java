package cn.jmicro.api.mng;

import cn.jmicro.api.annotation.SO;

@SO
public class ConfigNodeJRso {

	private String path;
	
	private String val;
	
	private String name;
	
	/*private Boolean updatable;
	
	private Boolean deleteable;
	
	private Boolean viewable;*/
	
	private ConfigNodeJRso[] children = null;
	
	public ConfigNodeJRso() {}
	
	public ConfigNodeJRso(String path,String val,String name) {
		this.path = path;
		this.val = val;
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}

	public ConfigNodeJRso[] getChildren() {
		return children;
	}

	public void setChildren(ConfigNodeJRso[] children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/*public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public boolean isDeleteable() {
		return deleteable;
	}

	public void setDeleteable(boolean deleteable) {
		this.deleteable = deleteable;
	}

	public boolean isViewable() {
		return viewable;
	}

	public void setViewable(boolean viewable) {
		this.viewable = viewable;
	}*/
	
}
