package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.SO;

@SO
public class MCConfigJRso {

	private int clientId;
	
	private String fieldName;
	
	private short type;
	
	private String label;
	
	private String desc;
	
	private String group;

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getLabel() {
		return label;
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

	public Short getType() {
		return type;
	}

	public void setType(Short type) {
		this.type = type;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MCConfigJRso other = (MCConfigJRso) obj;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MCConfig [fieldName=" + fieldName + ", type=" + type + ", label=" + label + ", desc=" + desc
				+ ", group=" + group + "]";
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public void setType(short type) {
		this.type = type;
	}

	
}
