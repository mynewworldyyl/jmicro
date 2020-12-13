package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.common.util.HashUtils;

@SO
public class CfgMetadata {

	private String name ="";
	private String resName="";
	private int dataType;
	private String defVal="";
	private String val;
	private int uiBoxType = CfgMetadata.UiType.Text.getCode();
	private boolean required = false;
	private boolean readonly = false;
	private boolean enable = false;
	private int hc = 0;
	private String validVals;
	
	public CfgMetadata() {}
	
	public CfgMetadata(String name,String group) {
		this.name = group;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getResName() {
		return resName;
	}

	public void setResName(String resName) {
		this.resName = resName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getDefVal() {
		return defVal;
	}

	public void setDefVal(String defVal) {
		this.defVal = defVal;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}

	public int getUiBoxType() {
		return uiBoxType;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setUiBoxType(int uiBoxType) {
		this.uiBoxType = uiBoxType;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getValidVals() {
		return validVals;
	}

	public void setValidVals(String validVals) {
		this.validVals = validVals;
	}

	@Override
	public int hashCode() {
		if(hc == 0) {
			this.hc = HashUtils.FNVHash1(this.resName+"."+this.name);
		}
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CfgMetadata)) {
			return false;
		}else {
			return  hashCode() == obj.hashCode();
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		CfgMetadata cm = (CfgMetadata)super.clone();
		cm.setDataType(this.dataType);
		cm.setDefVal(this.defVal);
		cm.setResName(this.getResName());
		cm.setName(this.name);
		cm.setRequired(this.isRequired());
		cm.setUiBoxType(this.getUiBoxType());
		cm.setVal(this.val);
		return cm;
	}

	@Override
	public String toString() {
		return "CfgMetadata [name=" + name + ", group=" + resName + ", dataType=" + dataType + ", defVal=" + defVal
				+ ", val=" + val + ", uiBoxType=" + uiBoxType + ", required=" + required + "]";
	}

	public enum UiType {
		Select(4),
		Radiobox(3),
		Checkbox(2),
		Text(1);
		
		private int code;
		UiType(int c) {
			this.code = c;
		}
		public int getCode() {
			return code;
		}
	}
	
	public enum DataType {
		Integer(4),
		Float(3),
		Boolean(2),
		String(1);
		
		private int code;
		DataType(int c) {
			this.code = c;
		}
		public int getCode() {
			return code;
		}
	}
	
}
