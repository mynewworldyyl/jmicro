package org.jmicro.api.mng;

import org.jmicro.api.annotation.SO;

@SO
public class ConfigNode {

	private String path;
	
	private String val;
	
	private String name;
	
	private ConfigNode[] children = null;
	
	public ConfigNode() {}
	
	public ConfigNode(String path,String val,String name) {
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

	public ConfigNode[] getChildren() {
		return children;
	}

	public void setChildren(ConfigNode[] children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}
