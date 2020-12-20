package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.common.util.HashUtils;

@SO
public class CfgMetadata {

	private String name ="";
	private String resName="";
	private int dataType;
	private String desc="";
	/*private String defVal="";
	private String val;
	private int uiBoxType = CfgMetadata.UiType.Text.getCode();
	private boolean required = false;
	private boolean readonly = false;
	private boolean enable = false;
	
	private String validVals;*/
	
	private transient int hc = 0;
	
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

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
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
		cm.setResName(this.getResName());
		cm.setName(this.name);
		return cm;
	}

	

	@Override
	public String toString() {
		return "CfgMetadata [name=" + name + ", resName=" + resName + ", dataType=" + dataType + "]";
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
