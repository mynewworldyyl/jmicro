package cn.jmicro.resource;

import cn.jmicro.api.annotation.SO;
import lombok.Serial;

@SO
@Serial
public class ResourceDataReqJRso {

	private int toType;
	
	private String[] resNames;
	
	private String[] insNames;
	
	private String host;
	
	private long startTime;
	
	private long endTime;
	
	private String tag;
	
	private int configId;
	
	private String groupBy;
	
	private int pageSize;
	
	private int curPage;

	public int getToType() {
		return toType;
	}

	public void setToType(int toType) {
		this.toType = toType;
	}

	public String[] getResNames() {
		return resNames;
	}

	public void setResNames(String[] resNames) {
		this.resNames = resNames;
	}

	public String[] getInsNames() {
		return insNames;
	}

	public void setInsNames(String[] insNames) {
		this.insNames = insNames;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public int getConfigId() {
		return configId;
	}

	public void setConfigId(int configId) {
		this.configId = configId;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getCurPage() {
		return curPage;
	}

	public void setCurPage(int curPage) {
		this.curPage = curPage;
	}
}
