package cn.jmicro.choreography.ha;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;

@SO
@IDStrategy(10)
public class MasterStatus {
	
	private int id;

	private int statuCode = 0;
	
	private String msg;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getStatuCode() {
		return statuCode;
	}

	public void setStatuCode(int statuCode) {
		this.statuCode = statuCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	
}
