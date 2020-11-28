package cn.jmicro.api.monitor;

import java.util.Map;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;

@SO
@IDStrategy(100)
public class StatisData {

	private int cid;
	
	private Map<String,Object> statis;
	
	private long inputTime;
	
	private String key;
	
	private String actName;
	
	private int type;
	
	public <T> T getIndex(String name) {
		return (T) statis.get(name);
	}

	public int getCid() {
		return cid;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public Map<String, Object> getStatis() {
		return statis;
	}

	public void setStatis(Map<String, Object> statis) {
		this.statis = statis;
	}

	public long getInputTime() {
		return inputTime;
	}

	public void setInputTime(long inputTime) {
		this.inputTime = inputTime;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getActName() {
		return actName;
	}

	public void setActName(String actName) {
		this.actName = actName;
	}
}
