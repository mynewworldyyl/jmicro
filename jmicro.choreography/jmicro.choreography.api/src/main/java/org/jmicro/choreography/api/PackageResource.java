package org.jmicro.choreography.api;

import org.jmicro.api.annotation.SO;

@SO
public class PackageResource {

	private String name;

	private long size;
	
	private String status;
	
	private boolean finish = true;
	
	private long offset;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean isFinish) {
		this.finish = isFinish;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	
}
