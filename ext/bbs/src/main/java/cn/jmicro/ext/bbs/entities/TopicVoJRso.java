package cn.jmicro.ext.bbs.entities;

import java.util.List;

import cn.jmicro.api.annotation.SO;
import lombok.Serial;

@SO
@Serial
public class TopicVoJRso {
	
	private TopicJRso topic;
	
	private List<NoteJRso> notes;

	public TopicJRso getTopic() {
		return topic;
	}

	public void setTopic(TopicJRso topic) {
		this.topic = topic;
	}

	public List<NoteJRso> getNotes() {
		return notes;
	}

	public void setNotes(List<NoteJRso> notes) {
		this.notes = notes;
	}
	
	
}
