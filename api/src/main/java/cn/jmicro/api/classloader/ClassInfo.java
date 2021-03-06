package cn.jmicro.api.classloader;

import cn.jmicro.api.annotation.SO;

@SO
public class ClassInfo {

	private String clazzName;
	
	private String jarFileName;
	
	private int dataVersion;
	
	private boolean testing;
	
	private long time = System.currentTimeMillis();

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

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
	
	
}
