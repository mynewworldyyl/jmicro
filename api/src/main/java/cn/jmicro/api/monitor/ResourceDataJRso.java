package cn.jmicro.api.monitor;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;

@SO
public class ResourceDataJRso {

	private int clientId;
	
	private int belongInsId;
	
	private String belongInsName;
	
	private String resName = "";
	
	private String httpHost = "";
	
	private String socketHost = "";
	
	private long time;
	
	private int cid;
	
	private String tag;
	
	private String osName;
	
	private Map<String,Object> metaData = new HashMap<>();
	
	public void putData(String key,Object val) {
		this.metaData.put(key, val);
	}
	
	public Object getData(String key) {
		return this.metaData.get(key);
	}
	
	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
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

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
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
		if(!(obj instanceof ResourceDataJRso)) {
			return false;
		}
		ResourceDataJRso rd = (ResourceDataJRso)obj;
		return this.resName.equals(rd.resName);
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getCid() {
		return cid;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getHttpHost() {
		return httpHost;
	}

	public void setHttpHost(String httpHost) {
		this.httpHost = httpHost;
	}

	public String getSocketHost() {
		return socketHost;
	}

	public void setSocketHost(String socketHost) {
		this.socketHost = socketHost;
	}
}
