package cn.jmicro.mng.api;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.pubsub.PSDataJRso;
import lombok.Serial;

@SO
@Serial
public class PSDataVoJRso extends PSDataJRso {

	private static final long serialVersionUID = 323374730999L;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int result=0;
	
	private PSDataJRso psData;
	
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

	public PSDataJRso getPsData() {
		return psData;
	}

	public void setPsData(PSDataJRso psData) {
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
