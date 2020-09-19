package cn.jmicro.ext.bbs.entities;

import java.util.List;

import cn.jmicro.api.annotation.SO;

@SO
public class TopicVo {
	
	private Topic topic;
	
	private List<Note> notes;

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	public List<Note> getNotes() {
		return notes;
	}

	public void setNotes(List<Note> notes) {
		this.notes = notes;
	}
	
	
}
