package cn.jmicro.api;

import lombok.Data;
import lombok.Serial;

@Data
@Serial
public abstract class PersistVo {

	private long createdTime;
	
	private long updatedTime;
	
	private int clientId;
	
	private long id;
	
	private int createdBy;
	
	private int updatedBy;
	
	//private boolean deleted;
	
}
