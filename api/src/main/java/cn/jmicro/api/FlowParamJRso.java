package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class FlowParamJRso {

	//参数名称
	private String name;
	
	//参数值
	private Object val;
	
	//参数描述
	private String desc;
	
	//参数值类型
	private byte type;
	
	private String clazz;
	
	public FlowParamJRso() {}
	
	public FlowParamJRso(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlowParamJRso other = (FlowParamJRso) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	
}
