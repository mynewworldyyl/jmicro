package cn.jmicro.ext.bbs.entities;

import java.util.List;

import cn.jmicro.api.annotation.SO;

@SO
public class TopicVo {
	
	private String createrId;

	private String createrName;
	
	private List<NoteVo> notes;

	public String getCreaterId() {
		return createrId;
	}

	public void setCreaterId(String createrId) {
		this.createrId = createrId;
	}

	public String getCreaterName() {
		return createrName;
	}

	public void setCreaterName(String createrName) {
		this.createrName = createrName;
	}

	public List<NoteVo> getNotes() {
		return notes;
	}

	public void setNotes(List<NoteVo> notes) {
		this.notes = notes;
	}
	
	
}
