package cn.jmicro.ext.bbs.entities;

import cn.jmicro.api.annotation.SO;

@SO
public class Note {

	private Long id;
	
	private int clientId;
	
    private String content;
	
	private long topicId;
	
	private long forNote=0;
	
    private int supportNum=0;
	
    private int opposeNum=0;
	
    private int seq=0;
	
	//举报
	//private int accusation;
	
	private int createdBy;
	
	private int updatedBy;
	
	private long createdTime;

	private long updatedTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getTopicId() {
		return topicId;
	}

	public void setTopicId(long topicId) {
		this.topicId = topicId;
	}

	public long getForNote() {
		return forNote;
	}

	public void setForNote(long forNote) {
		this.forNote = forNote;
	}

	public int getSupportNum() {
		return supportNum;
	}

	public void setSupportNum(int supportNum) {
		this.supportNum = supportNum;
	}

	public int getOpposeNum() {
		return opposeNum;
	}

	public void setOpposeNum(int opposeNum) {
		this.opposeNum = opposeNum;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public int getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(int updatedBy) {
		this.updatedBy = updatedBy;
	}

	public long getCreatedTime() {
		return createdTime;
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

	
    
}
