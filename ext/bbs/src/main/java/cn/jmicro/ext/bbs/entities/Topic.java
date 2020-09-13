package cn.jmicro.ext.bbs.entities;

import cn.jmicro.api.annotation.SO;

@SO
public class Topic {

	private long id;
	
	private int clientId;
	
    private String content;
	
    private String title;
	
    private int readNum=0;
	
    private int noteNum=0;
	
    private boolean resolved=false;
	
    private boolean locked = false;
	
    private boolean recall = false;	
	
    private int topSeq=0;
	
    private boolean firstTopic=false;
	
    private boolean essence=false;
	
	private String topicType;

    //举报
  	private int accusation;
  	
  	private int createdBy;
  	
  	private int updatedBy;
  	
    private long createdTime;

    private long updatedTime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getReadNum() {
		return readNum;
	}

	public void setReadNum(int readNum) {
		this.readNum = readNum;
	}

	public int getNoteNum() {
		return noteNum;
	}

	public void setNoteNum(int noteNum) {
		this.noteNum = noteNum;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isRecall() {
		return recall;
	}

	public void setRecall(boolean recall) {
		this.recall = recall;
	}

	public int getTopSeq() {
		return topSeq;
	}

	public void setTopSeq(int topSeq) {
		this.topSeq = topSeq;
	}

	public boolean isFirstTopic() {
		return firstTopic;
	}

	public void setFirstTopic(boolean firstTopic) {
		this.firstTopic = firstTopic;
	}

	public boolean isEssence() {
		return essence;
	}

	public void setEssence(boolean essence) {
		this.essence = essence;
	}

	public String getTopicType() {
		return topicType;
	}

	public void setTopicType(String topicType) {
		this.topicType = topicType;
	}

	public int getAccusation() {
		return accusation;
	}

	public void setAccusation(int accusation) {
		this.accusation = accusation;
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
