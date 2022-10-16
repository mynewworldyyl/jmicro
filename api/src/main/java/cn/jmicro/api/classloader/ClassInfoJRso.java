package cn.jmicro.api.classloader;

import cn.jmicro.common.Constants;
import lombok.Serial;

@Serial
public class ClassInfoJRso {

	private String clazzName;
	
	private String jarFileName;
	
	private int dataVersion;
	
	private boolean testing;
	
	private long modifiedTime;
	
	private int clientId = Constants.NO_CLIENT_ID;

	public String getClazzName() {
		return clazzName;
	}

	public void setClazzName(String clazzName) {
		this.clazzName = clazzName;
	}

	public String getJarFileName() {
		return jarFileName;
	}

	public void setJarFileName(String jarFileName) {
		this.jarFileName = jarFileName;
	}

	public int getDataVersion() {
		return dataVersion;
	}

	public void setDataVersion(int dataVersion) {
		this.dataVersion = dataVersion;
	}

	public boolean isTesting() {
		return testing;
	}

	public void setTesting(boolean testing) {
		this.testing = testing;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public void setModifiedTime(long modifiedTime) {
		this.modifiedTime = modifiedTime;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	
	
}
