package cn.jmicro.api.monitor;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;

@SO
public class ResourceData {

	private int belongInsId;
	
	private String belongInsName;
	
	private String resName = "";
	
	private Map<String,Object> metaData = new HashMap<>();
	
	public void putData(String key,Object val) {
		this.metaData.put(key, val);
	}
	
	public Object getData(String key) {
		return this.metaData.get(key);
	}
	
	public <T> T removeData(String key) {
		return (T)this.metaData.remove(key);
	}

	public int getBelongInsId() {
		return belongInsId;
	}

	public void setBelongInsId(int belongInsId) {
		this.belongInsId = belongInsId;
	}

	public String getBelongInsName() {
		return belongInsName;
	}

	public void setBelongInsName(String belongInsName) {
		this.belongInsName = belongInsName;
	}

	public String getResName() {
		return resName;
	}

	public void setResName(String resName) {
		this.resName = resName;
	}

	public Map<String, Object> getMetaData() {
		return metaData;
	}

	public void setMetaData(Map<String, Object> metaData) {
		this.metaData = metaData;
	}

	@Override
	public int hashCode() {
		return this.resName == null ? "".hashCode():this.resName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ResourceData)) {
			return false;
		}
		ResourceData rd = (ResourceData)obj;
		return this.resName.equals(rd.resName);
	}
	
	
}
