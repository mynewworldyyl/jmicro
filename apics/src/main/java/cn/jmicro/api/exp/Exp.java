package cn.jmicro.api.exp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Exp {

	private String oriEx;
	
	private List<String> suffix;
	
	public Set<String> vars = new HashSet<>();
	
	public Exp() {}
	
	public Exp(String oriEx) {
		this.oriEx = oriEx;
	}
	
	public String getOriEx() {
		return oriEx;
	}

	public void setOriEx(String oriEx) {
		this.oriEx = oriEx;
		if(this.suffix != null && !this.suffix.isEmpty()) {
			parseVar();
		}
	}

	private void parseVar() {
		for(String e : this.suffix) {
			if(ExpUtils.OPS.containsKey(e)
					|| e.startsWith("\"")
					|| e.equals("true") 
					|| e.equals("false")) {
				continue;
			} else {
				this.vars.add(e);
			}
		}
	}
	
	public boolean containerVar(String name) {
		return this.vars.contains(name);
	}

	public List<String> getSuffix() {
		return suffix;
	}

	public void setSuffix(List<String> suffix) {
		this.suffix = suffix;
	}
	
}
