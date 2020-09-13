package cn.jmicro.ext.bbs.entities;

import cn.jmicro.api.annotation.SO;

@SO
public class NoteVo {

	private Note note;
	
	private String createrId;

	private String createrName;

	public Note getNote() {
		return note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

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
	
	
}
