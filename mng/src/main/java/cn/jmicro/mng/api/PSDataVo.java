package cn.jmicro.mng.api;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.pubsub.PSData;

@SO
public class PSDataVo extends PSData {

	private static final long serialVersionUID = 323374730999L;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int result=0;
	
	private PSData psData;
	
	//private String _id;

	/*public void from(PSData psd) {
		this.setCallback(psd.getCallback());
		this.setContext(psd.getContext());
		this.setData(psd.getData());
		this.setFailCnt(psd.getFailCnt());
		this.setFlag(psd.getFlag());
		this.setId(psd.getId());
		this.setSrcClientId(psd.getSrcClientId());
		this.setTopic(psd.getTopic());
	}*/
	
	public long getCreatedTime() {
		return createdTime;
	}

	public PSData getPsData() {
		return psData;
	}

	public void setPsData(PSData psData) {
		this.psData = psData;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(long updatedTime) {
		this.updatedTime = updatedTime;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}
	
}
